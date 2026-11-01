package kr.flowdesk.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpSession;
import kr.flowdesk.domain.AppUser;
import kr.flowdesk.domain.InbodyRecord;
import kr.flowdesk.service.AppUserService;
import kr.flowdesk.service.FdAuthService;
import kr.flowdesk.service.InbodyService;
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

@RestController
@RequestMapping("/api/inbody")
public class InbodyController {

  private final InbodyService inbodyService;
  private final AppUserService appUserService;

  public InbodyController(InbodyService inbodyService, AppUserService appUserService) {
    this.inbodyService = inbodyService;
    this.appUserService = appUserService;
  }

  /** 01 권한: 조회 대상 사용자 목록 (콤보용) */
  @GetMapping("/users")
  public ResponseEntity<?> listUsers(HttpSession session) {
    if (!isAuthAdmin(session)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(error("전체관리자(01)만 다른 사용자 인바디를 조회할 수 있습니다."));
    }
    List<Map<String, Object>> list = appUserService.findAll().stream().map(u -> {
      Map<String, Object> m = new HashMap<>();
      m.put("username", u.getUsername());
      m.put("displayName", u.getDisplayName());
      m.put("authCd", u.getAuthCd());
      return m;
    }).collect(Collectors.toList());
    return ResponseEntity.ok(list);
  }

  @GetMapping
  public ResponseEntity<?> list(
      @RequestParam(value = "user", required = false) String user,
      HttpSession session) {
    String owner = resolveOwner(session, user);
    if (owner == null) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("권한이 없습니다."));
    }
    return ResponseEntity.ok(inbodyService.findByOwner(owner));
  }

  @GetMapping("/latest")
  public ResponseEntity<?> latest(
      @RequestParam(value = "user", required = false) String user,
      HttpSession session) {
    String owner = resolveOwner(session, user);
    if (owner == null) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("권한이 없습니다."));
    }
    InbodyRecord latest = inbodyService.findLatest(owner);
    if (latest == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(latest);
  }

  @GetMapping("/summary")
  public ResponseEntity<?> summary(
      @RequestParam(value = "user", required = false) String user,
      HttpSession session) {
    String owner = resolveOwner(session, user);
    if (owner == null) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("권한이 없습니다."));
    }
    Map<String, Object> res = new HashMap<>();
    List<InbodyRecord> all = inbodyService.findByOwner(owner);
    res.put("owner", owner);
    res.put("count", all.size());
    res.put("list", all);
    res.put("latest", all.isEmpty() ? null : all.get(all.size() - 1));
    if (all.size() >= 2) {
      InbodyRecord first = all.get(0);
      InbodyRecord last = all.get(all.size() - 1);
      Map<String, Object> delta = new HashMap<>();
      delta.put("weightKg", diff(last.getWeightKg(), first.getWeightKg()));
      delta.put("skeletalMuscleKg", diff(last.getSkeletalMuscleKg(), first.getSkeletalMuscleKg()));
      delta.put("bodyFatMassKg", diff(last.getBodyFatMassKg(), first.getBodyFatMassKg()));
      delta.put("percentBodyFat", diff(last.getPercentBodyFat(), first.getPercentBodyFat()));
      delta.put("bmi", diff(last.getBmi(), first.getBmi()));
      delta.put("fitnessScore", diffInt(last.getFitnessScore(), first.getFitnessScore()));
      delta.put("visceralFatLevel", diffInt(last.getVisceralFatLevel(), first.getVisceralFatLevel()));
      res.put("deltaFromFirst", delta);
    }
    return ResponseEntity.ok(res);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> get(@PathVariable String id, HttpSession session) {
    InbodyRecord found = inbodyService.findById(id);
    if (found == null) {
      return ResponseEntity.notFound().build();
    }
    if (!canAccessRecord(session, found)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("권한이 없습니다."));
    }
    return ResponseEntity.ok(found);
  }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody InbodyRecord record, HttpSession session) {
    String actor = AuthController.usernameOf(session);
    if (actor == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("로그인이 필요합니다."));
    }
    if (record.getMeasuredAt() == null || record.getMeasuredAt().trim().isEmpty()) {
      throw new IllegalArgumentException("측정일을 입력해주세요.");
    }
    if (record.getWeightKg() == null) {
      throw new IllegalArgumentException("체중을 입력해주세요.");
    }
    boolean admin = isAuthAdmin(session);
    String owner = record.getCreateUser();
    if (owner == null || owner.trim().isEmpty()) {
      owner = actor;
    } else if (!admin && !owner.equals(actor)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(error("본인 인바디만 등록할 수 있습니다."));
    }
    if (admin && !owner.equals(actor)) {
      AppUser target = appUserService.findByUsername(owner);
      if (target == null) {
        throw new IllegalArgumentException("대상 사용자를 찾을 수 없습니다: " + owner);
      }
    }
    record.setCreateUser(owner);
    return ResponseEntity.status(HttpStatus.CREATED).body(inbodyService.create(record, actor));
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(
      @PathVariable String id, @RequestBody InbodyRecord record, HttpSession session) {
    String actor = AuthController.usernameOf(session);
    if (actor == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("로그인이 필요합니다."));
    }
    try {
      return ResponseEntity.ok(inbodyService.update(id, record, actor, isAuthAdmin(session)));
    } catch (IllegalArgumentException e) {
      if (e.getMessage() != null && e.getMessage().contains("본인")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(e.getMessage()));
      }
      throw e;
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable String id, HttpSession session) {
    String actor = AuthController.usernameOf(session);
    if (actor == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("로그인이 필요합니다."));
    }
    try {
      inbodyService.delete(id, actor, isAuthAdmin(session));
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      if (e.getMessage() != null && e.getMessage().contains("본인")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(e.getMessage()));
      }
      throw e;
    }
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleBad(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(error(e.getMessage()));
  }

  /** 대상 사용자 결정. 비관리자는 본인만. */
  private String resolveOwner(HttpSession session, String requestedUser) {
    String actor = AuthController.usernameOf(session);
    if (actor == null) return null;
    if (requestedUser == null || requestedUser.trim().isEmpty() || requestedUser.equals(actor)) {
      return actor;
    }
    if (!isAuthAdmin(session)) {
      return null;
    }
    AppUser target = appUserService.findByUsername(requestedUser.trim());
    if (target == null) {
      throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + requestedUser);
    }
    return target.getUsername();
  }

  private boolean canAccessRecord(HttpSession session, InbodyRecord record) {
    String actor = AuthController.usernameOf(session);
    if (actor == null) return false;
    if (isAuthAdmin(session)) return true;
    return actor.equals(record.getCreateUser());
  }

  private boolean isAuthAdmin(HttpSession session) {
    String authCd = AuthController.authCdOf(session);
    return FdAuthService.AUTH_ADMIN.equals(authCd);
  }

  private Map<String, Object> error(String message) {
    Map<String, Object> body = new HashMap<>();
    body.put("ok", false);
    body.put("message", message);
    return body;
  }

  private Double diff(Double a, Double b) {
    if (a == null || b == null) return null;
    return Math.round((a - b) * 10.0) / 10.0;
  }

  private Integer diffInt(Integer a, Integer b) {
    if (a == null || b == null) return null;
    return a - b;
  }
}
