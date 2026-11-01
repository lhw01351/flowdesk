package kr.flowdesk.web.system;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import kr.flowdesk.domain.AppUser;
import kr.flowdesk.service.system.AccessLogService;
import kr.flowdesk.service.system.AppUserService;
import kr.flowdesk.service.system.FdAuthService;
import kr.flowdesk.service.system.MenuAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 회원 등록/관리 — admin·hwlee 전용 (AuthFilter isManager)
 */
@RestController
@RequestMapping("/api/users")
public class AppUserController {

  private final AppUserService appUserService;
  private final MenuAccessService menuAccessService;
  private final AccessLogService accessLogService;

  public AppUserController(
      AppUserService appUserService,
      MenuAccessService menuAccessService,
      AccessLogService accessLogService) {
    this.appUserService = appUserService;
    this.menuAccessService = menuAccessService;
    this.accessLogService = accessLogService;
  }

  @GetMapping
  public List<Map<String, Object>> list() {
    return appUserService.findAll().stream().map(this::safe).collect(Collectors.toList());
  }

  @PostMapping
  public ResponseEntity<Map<String, Object>> create(@RequestBody AppUser body) {
    return ResponseEntity.status(HttpStatus.CREATED).body(safe(appUserService.create(body)));
  }

  @PutMapping("/{id}")
  public Map<String, Object> update(@PathVariable String id, @RequestBody AppUser body) {
    return safe(appUserService.update(id, body));
  }

  /** 회원 프로필 사진 업로드 (관리자) */
  @PostMapping("/{id}/avatar")
  public Map<String, Object> uploadAvatar(
      @PathVariable String id, @RequestParam("file") MultipartFile file) throws IOException {
    return safe(appUserService.saveProfileImage(id, file));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id, HttpSession session) {
    AppUser target = appUserService.findById(id);
    if (target == null) {
      return ResponseEntity.notFound().build();
    }
    Object me = session.getAttribute(AuthController.SESSION_USER);
    if (me != null && me.equals(target.getUsername())) {
      throw new IllegalArgumentException("자기 자신 계정은 삭제할 수 없습니다.");
    }
    appUserService.delete(id);
    return ResponseEntity.noContent().build();
  }

  /** 관리자가 해당 회원으로 세션을 전환 (비밀번호 없이) */
  @PostMapping("/{id}/login")
  public ResponseEntity<Map<String, Object>> loginAs(
      @PathVariable String id, HttpSession session, HttpServletRequest request) {
    if (!MenuAccessService.isManager(session)) {
      Map<String, Object> deny = new HashMap<>();
      deny.put("ok", false);
      deny.put("message", "권한이 없습니다.");
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(deny);
    }
    AppUser target = appUserService.findById(id);
    if (target == null) {
      Map<String, Object> notFound = new HashMap<>();
      notFound.put("ok", false);
      notFound.put("message", "사용자를 찾을 수 없습니다.");
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFound);
    }

    String adminUser = AuthController.usernameOf(session);
    String username = target.getUsername();
    String authCd = target.getAuthCd() == null || target.getAuthCd().isEmpty()
        ? FdAuthService.AUTH_USER : target.getAuthCd();
    String role = AppUserService.roleFromAuthCd(authCd);
    String displayName = target.getDisplayName();

    session.setAttribute(AuthController.SESSION_USER, username);
    session.setAttribute(AuthController.SESSION_ROLE, role);
    session.setAttribute(AuthController.SESSION_AUTH_CD, authCd);
    if (displayName != null) {
      session.setAttribute(AuthController.SESSION_DISPLAY, displayName);
    } else {
      session.removeAttribute(AuthController.SESSION_DISPLAY);
    }

    try {
      accessLogService.record(
          username,
          "LOGIN_AS",
          "/users.html",
          "대리 로그인",
          null,
          null,
          null,
          "by:" + (adminUser == null ? "" : adminUser),
          request);
    } catch (Exception ignored) {
    }

    Map<String, Object> res = new HashMap<>();
    res.put("ok", true);
    res.put("username", username);
    res.put("displayName", displayName);
    res.put("role", role);
    res.put("authCd", authCd);
    res.putAll(menuAccessService.menuPayload(username, authCd));
    res.put("message", username + " 계정으로 로그인되었습니다.");
    return ResponseEntity.ok(res);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> bad(IllegalArgumentException e) {
    Map<String, Object> body = new HashMap<>();
    body.put("ok", false);
    body.put("message", e.getMessage());
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(IOException.class)
  public ResponseEntity<Map<String, Object>> io(IOException e) {
    Map<String, Object> body = new HashMap<>();
    body.put("ok", false);
    body.put("message", "파일 저장에 실패했습니다.");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  private Map<String, Object> safe(AppUser u) {
    Map<String, Object> m = new HashMap<>();
    m.put("id", u.getId());
    m.put("username", u.getUsername());
    m.put("role", u.getRole());
    m.put("authCd", u.getAuthCd());
    m.put("authNm", u.getAuthNm());
    m.put("displayName", u.getDisplayName());
    m.put("createdAt", u.getCreatedAt());
    m.put("note", u.getNote());
    boolean has = AppUserService.hasProfileImage(u);
    m.put("hasAvatar", has);
    m.put("avatarUrl", has ? "/api/auth/avatar/" + u.getUsername() : null);
    return m;
  }
}
