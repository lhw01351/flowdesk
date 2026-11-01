package kr.flowdesk.web;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import kr.flowdesk.config.AuthProperties;
import kr.flowdesk.config.AuthUser;
import kr.flowdesk.domain.AppUser;
import kr.flowdesk.service.AccessLogService;
import kr.flowdesk.service.AppUserService;
import kr.flowdesk.service.FdAuthService;
import kr.flowdesk.service.MenuAccessService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인 / 로그아웃 / 세션 확인
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  public static final String SESSION_USER = "CX_AUTH_USER";
  public static final String SESSION_ROLE = "CX_AUTH_ROLE";
  public static final String SESSION_AUTH_CD = "FD_AUTH_CD";
  public static final String SESSION_DISPLAY = "FD_DISPLAY_NAME";

  public static final String ROLE_ADMIN = "ADMIN";
  /** @deprecated auth_cd 01/02 사용. 구 데이터 호환용 */
  public static final String ROLE_WORK = "WORK";
  public static final String ROLE_GUEST = "GUEST";

  private final AuthProperties authProperties;
  private final AppUserService appUserService;
  private final AccessLogService accessLogService;
  private final MenuAccessService menuAccessService;

  public AuthController(
      AuthProperties authProperties,
      AppUserService appUserService,
      AccessLogService accessLogService,
      MenuAccessService menuAccessService) {
    this.authProperties = authProperties;
    this.appUserService = appUserService;
    this.accessLogService = accessLogService;
    this.menuAccessService = menuAccessService;
  }

  @PostMapping("/login")
  public ResponseEntity<Map<String, Object>> login(
      @RequestBody Map<String, Object> body, HttpSession session, HttpServletRequest request) {
    String id = body.get("username") == null ? "" : String.valueOf(body.get("username")).trim();
    String pw = body.get("password") == null ? "" : String.valueOf(body.get("password"));

    Map<String, Object> res = new HashMap<>();
    if (id.isEmpty() || pw.isEmpty()) {
      res.put("ok", false);
      res.put("message", "아이디와 비밀번호를 입력해주세요.");
      return ResponseEntity.badRequest().body(res);
    }

    String role;
    String authCd;
    String displayName = null;

    AppUser dbUser = appUserService.authenticate(id, pw);
    if (dbUser != null) {
      id = dbUser.getUsername();
      authCd = dbUser.getAuthCd() == null || dbUser.getAuthCd().isEmpty()
          ? FdAuthService.AUTH_USER : dbUser.getAuthCd();
      role = AppUserService.roleFromAuthCd(authCd);
      displayName = dbUser.getDisplayName();
    } else {
      AuthUser ymlUser = authProperties.findByUsername(id);
      if (ymlUser == null || ymlUser.getPassword() == null || !ymlUser.getPassword().equals(pw)) {
        res.put("ok", false);
        res.put("message", "아이디 또는 비밀번호가 올바르지 않습니다.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
      }
      role = ymlUser.getRole() == null ? ROLE_ADMIN : ymlUser.getRole().trim().toUpperCase();
      authCd = ROLE_GUEST.equals(role) ? FdAuthService.AUTH_USER : FdAuthService.AUTH_ADMIN;
    }

    session.setAttribute(SESSION_USER, id);
    session.setAttribute(SESSION_ROLE, role);
    session.setAttribute(SESSION_AUTH_CD, authCd);
    if (displayName != null) {
      session.setAttribute(SESSION_DISPLAY, displayName);
    }

    Double lat = toDouble(body.get("latitude"));
    Double lng = toDouble(body.get("longitude"));
    Double acc = toDouble(body.get("accuracy"));
    String gpsStatus = body.get("gpsStatus") == null ? null : String.valueOf(body.get("gpsStatus"));
    try {
      accessLogService.record(id, "LOGIN", "/login.html", "로그인", lat, lng, acc, gpsStatus, request);
    } catch (Exception ignored) {
    }

    res.put("ok", true);
    res.put("username", id);
    res.put("displayName", displayName);
    res.put("role", role);
    res.put("authCd", authCd);
    res.putAll(menuAccessService.menuPayload(id, authCd));
    putSessionInfo(res, session);
    putAvatarInfo(res, id);
    res.put("message", "로그인 성공");
    return ResponseEntity.ok(res);
  }

  @PostMapping("/logout")
  public Map<String, Object> logout(HttpSession session, HttpServletRequest request) {
    String user = usernameOf(session);
    try {
      if (user != null) {
        accessLogService.record(user, "LOGOUT", "/logout", "로그아웃", null, null, null, null, request);
      }
    } catch (Exception ignored) {
    }
    session.invalidate();
    Map<String, Object> res = new HashMap<>();
    res.put("ok", true);
    return res;
  }

  @GetMapping("/me")
  public ResponseEntity<Map<String, Object>> me(HttpSession session) {
    Object user = session.getAttribute(SESSION_USER);
    Map<String, Object> res = new HashMap<>();
    if (user == null) {
      res.put("ok", false);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
    }
    String username = String.valueOf(user);
    String role = String.valueOf(session.getAttribute(SESSION_ROLE));
    if (role == null || "null".equals(role)) {
      role = ROLE_GUEST;
    }
    Object authObj = session.getAttribute(SESSION_AUTH_CD);
    String authCd = authObj == null ? FdAuthService.AUTH_USER : String.valueOf(authObj);
    Object display = session.getAttribute(SESSION_DISPLAY);

    // DB에 최신 표시명·아바타 반영
    AppUser dbUser = appUserService.findByUsername(username);
    if (dbUser != null && dbUser.getDisplayName() != null && !dbUser.getDisplayName().isEmpty()) {
      display = dbUser.getDisplayName();
      session.setAttribute(SESSION_DISPLAY, display);
    }

    res.put("ok", true);
    res.put("username", username);
    res.put("displayName", display);
    res.put("role", role);
    res.put("authCd", authCd);
    res.putAll(menuAccessService.menuPayload(username, authCd));
    putSessionInfo(res, session);
    putAvatarInfo(res, username);
    return ResponseEntity.ok(res);
  }

  @GetMapping("/avatar/{username}")
  public ResponseEntity<Resource> avatar(@PathVariable String username, HttpSession session) {
    if (usernameOf(session) == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (username == null || username.trim().isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    AppUser user = appUserService.findByUsername(username.trim());
    Resource resource = appUserService.loadProfileImage(user);
    if (resource == null) {
      return ResponseEntity.notFound().build();
    }
    MediaType type = MediaType.parseMediaType(appUserService.contentTypeFor(user));
    return ResponseEntity.ok()
        .contentType(type)
        .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
        .body(resource);
  }

  private void putSessionInfo(Map<String, Object> res, HttpSession session) {
    int maxSec = session.getMaxInactiveInterval();
    long last = session.getLastAccessedTime();
    long remainingMs = Math.max(0L, last + maxSec * 1000L - System.currentTimeMillis());
    res.put("sessionMaxInactiveSec", maxSec);
    res.put("sessionRemainingSec", remainingMs / 1000L);
  }

  private void putAvatarInfo(Map<String, Object> res, String username) {
    AppUser user = appUserService.findByUsername(username);
    if (AppUserService.hasProfileImage(user)) {
      res.put("avatarUrl", "/api/auth/avatar/" + user.getUsername());
      res.put("hasAvatar", true);
    } else {
      res.put("avatarUrl", null);
      res.put("hasAvatar", false);
    }
  }

  public static String authCdOf(HttpSession session) {
    if (session == null) return null;
    Object o = session.getAttribute(SESSION_AUTH_CD);
    return o == null ? null : String.valueOf(o);
  }

  public static String usernameOf(HttpSession session) {
    if (session == null) return null;
    Object o = session.getAttribute(SESSION_USER);
    return o == null ? null : String.valueOf(o);
  }

  private Double toDouble(Object o) {
    if (o == null || "".equals(o)) return null;
    if (o instanceof Number) return ((Number) o).doubleValue();
    try {
      return Double.parseDouble(String.valueOf(o));
    } catch (Exception e) {
      return null;
    }
  }
}
