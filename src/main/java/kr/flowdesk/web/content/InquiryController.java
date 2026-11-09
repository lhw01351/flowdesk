package kr.flowdesk.web.content;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpSession;
import kr.flowdesk.domain.AppUser;
import kr.flowdesk.domain.Inquiry;
import kr.flowdesk.service.system.AppUserService;
import kr.flowdesk.service.system.FdAuthService;
import kr.flowdesk.service.content.InquiryService;
import kr.flowdesk.web.system.AuthController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

/**
 * 문의 REST API — 사용자별 소유. 전체관리자(01)만 타인 조회/등록
 */
@RestController
@RequestMapping("/api/inquiries")
public class InquiryController {

  private final InquiryService inquiryService;
  private final AppUserService appUserService;

  public InquiryController(InquiryService inquiryService, AppUserService appUserService) {
    this.inquiryService = inquiryService;
    this.appUserService = appUserService;
  }

  /** 01 권한: 조회 대상 사용자 목록 (콤보용) */
  @GetMapping("/users")
  public ResponseEntity<?> listUsers(HttpSession session) {
    if (!isAuthAdmin(session)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(error("전체관리자(01)만 다른 사용자 문의를 조회할 수 있습니다."));
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
    return ResponseEntity.ok(inquiryService.findByOwner(owner));
  }

  @GetMapping("/bundle")
  public ResponseEntity<?> bundle(
      @RequestParam(value = "user", required = false) String user,
      HttpSession session) {
    String owner = resolveOwner(session, user);
    if (owner == null) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("권한이 없습니다."));
    }
    return ResponseEntity.ok(inquiryService.listWithSla(owner));
  }

  @GetMapping("/stats")
  public ResponseEntity<?> stats(
      @RequestParam(value = "user", required = false) String user,
      HttpSession session) {
    String owner = resolveOwner(session, user);
    if (owner == null) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("권한이 없습니다."));
    }
    return ResponseEntity.ok(inquiryService.stats(owner));
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> get(@PathVariable String id, HttpSession session) {
    Inquiry found = inquiryService.findById(id);
    if (found == null) {
      return ResponseEntity.notFound().build();
    }
    if (!canAccessRecord(session, found)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("권한이 없습니다."));
    }
    return ResponseEntity.ok(found);
  }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody Inquiry inquiry, HttpSession session) {
    String actor = AuthController.usernameOf(session);
    if (actor == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("로그인이 필요합니다."));
    }
    boolean admin = isAuthAdmin(session);
    String owner = inquiry.getCreateUser();
    if (owner == null || owner.trim().isEmpty()) {
      owner = actor;
    } else if (!admin && !owner.equals(actor)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(error("본인 문의만 등록할 수 있습니다."));
    }
    if (admin && !owner.equals(actor)) {
      AppUser target = appUserService.findByUsername(owner);
      if (target == null) {
        throw new IllegalArgumentException("대상 사용자를 찾을 수 없습니다: " + owner);
      }
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(inquiryService.create(inquiry, owner));
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(
      @PathVariable String id, @RequestBody Inquiry inquiry, HttpSession session) {
    String actor = AuthController.usernameOf(session);
    if (actor == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("로그인이 필요합니다."));
    }
    try {
      return ResponseEntity.ok(inquiryService.update(id, inquiry, actor, isAuthAdmin(session)));
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
      inquiryService.delete(id, actor, isAuthAdmin(session));
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      if (e.getMessage() != null && e.getMessage().contains("본인")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(e.getMessage()));
      }
      throw e;
    }
  }

  /**
   * localStorage / CSV JSON 일괄 이관
   * body: { "replace": true, "list": [ ... ], "createUser": "optional" }
   */
  @PostMapping("/import")
  public ResponseEntity<?> importJson(@RequestBody Map<String, Object> body, HttpSession session) {
    String actor = AuthController.usernameOf(session);
    if (actor == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("로그인이 필요합니다."));
    }
    String requested = body.get("createUser") == null ? null : String.valueOf(body.get("createUser"));
    String owner;
    try {
      owner = resolveOwner(session, requested);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(error(e.getMessage()));
    }
    if (owner == null) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("권한이 없습니다."));
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> rawList = (List<Map<String, Object>>) body.get("list");
    boolean replace = body.get("replace") == null || Boolean.TRUE.equals(body.get("replace"));
    List<Inquiry> list = new java.util.ArrayList<>();
    if (rawList != null) {
      for (Map<String, Object> m : rawList) {
        list.add(mapToInquiry(m));
      }
    }
    int count = inquiryService.importAll(list, replace, owner);
    Map<String, Object> res = new HashMap<>();
    res.put("imported", count);
    res.put("replace", replace);
    res.put("owner", owner);
    return ResponseEntity.ok(res);
  }

  @GetMapping(value = "/export.csv", produces = "text/csv")
  public ResponseEntity<?> exportCsv(
      @RequestParam(value = "user", required = false) String user,
      HttpSession session) {
    String owner = resolveOwner(session, user);
    if (owner == null) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("권한이 없습니다."));
    }
    List<Inquiry> list = inquiryService.findByOwner(owner);
    StringBuilder sb = new StringBuilder();
    sb.append('\uFEFF');
    sb.append("구분,채널,상태,유형,모듈,인입일시,1차응대일시,처리일시,고객사명,확인자,비고,연관문의ID\n");
    for (Inquiry d : list) {
      sb.append(csv(d.getDirection())).append(',')
        .append(csv(d.getChannel())).append(',')
        .append(csv(d.getStatus())).append(',')
        .append(csv(d.getCategory())).append(',')
        .append(csv(d.getModule())).append(',')
        .append(csv(d.getInboundAt())).append(',')
        .append(csv(d.getFirstResponseAt())).append(',')
        .append(csv(d.getResolvedAt())).append(',')
        .append(csv(d.getCustomer())).append(',')
        .append(csv(d.getReviewer())).append(',')
        .append(csv(d.getNote())).append(',')
        .append(csv(d.getLinkedId())).append('\n');
    }
    byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cx_inquiries.csv\"")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(bytes);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(error(e.getMessage()));
  }

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

  private boolean canAccessRecord(HttpSession session, Inquiry record) {
    String actor = AuthController.usernameOf(session);
    if (actor == null) return false;
    if (canSelectOtherOwners(session)) return true;
    return actor.equals(record.getCreateUser());
  }

  /** 타인 문의 조회/대리등록 — 권한 01 */
  private boolean canSelectOtherOwners(HttpSession session) {
    return FdAuthService.AUTH_ADMIN.equals(AuthController.authCdOf(session));
  }

  private boolean isAuthAdmin(HttpSession session) {
    return canSelectOtherOwners(session);
  }

  private Map<String, Object> error(String message) {
    Map<String, Object> body = new HashMap<>();
    body.put("ok", false);
    body.put("message", message);
    return body;
  }

  private Inquiry mapToInquiry(Map<String, Object> m) {
    Inquiry in = new Inquiry();
    in.setId(str(m.get("id")));
    in.setDirection(str(m.get("direction")));
    in.setChannel(str(m.get("channel")));
    in.setStatus(str(m.get("status")));
    in.setCategory(str(m.get("category")));
    in.setModule(str(m.get("module")));
    in.setInboundAt(str(m.get("inboundAt")));
    in.setFirstResponseAt(str(m.get("firstResponseAt")));
    in.setResolvedAt(str(m.get("resolvedAt")));
    in.setCustomer(str(m.get("customer")));
    in.setReviewer(str(m.get("reviewer")));
    in.setNote(str(m.get("note")));
    in.setLinkedId(str(m.get("linkedId")));
    return in;
  }

  private String str(Object o) {
    return o == null ? "" : String.valueOf(o);
  }

  private String csv(String v) {
    String s = v == null ? "" : v;
    return "\"" + s.replace("\"", "\"\"") + "\"";
  }
}
