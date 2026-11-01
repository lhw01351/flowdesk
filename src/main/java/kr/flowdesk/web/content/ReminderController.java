package kr.flowdesk.web.content;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;
import kr.flowdesk.domain.Reminder;
import kr.flowdesk.service.content.ReminderService;
import kr.flowdesk.web.system.AuthController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
@RequestMapping("/api/reminders")
public class ReminderController {

  private final ReminderService reminderService;

  public ReminderController(ReminderService reminderService) {
    this.reminderService = reminderService;
  }

  @GetMapping
  public ResponseEntity<?> list(
      @RequestParam(value = "from", required = false) String from,
      @RequestParam(value = "to", required = false) String to,
      @RequestParam(value = "year", required = false) Integer year,
      @RequestParam(value = "month", required = false) Integer month,
      HttpSession session) {
    String user = currentUser(session);
    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("로그인이 필요합니다."));
    }
    String[] range = resolveRange(from, to, year, month);
    List<Reminder> list = reminderService.findRange(user, range[0], range[1]);
    Map<String, Object> body = new HashMap<>();
    body.put("username", user);
    body.put("from", range[0]);
    body.put("to", range[1]);
    body.put("list", list);
    return ResponseEntity.ok(body);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> one(@PathVariable String id, HttpSession session) {
    String user = currentUser(session);
    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("로그인이 필요합니다."));
    }
    Reminder row = reminderService.findById(id);
    if (row == null || !user.equals(row.getUsername())) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("없습니다."));
    }
    return ResponseEntity.ok(row);
  }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody Reminder body, HttpSession session) {
    String user = currentUser(session);
    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("로그인이 필요합니다."));
    }
    return ResponseEntity.ok(reminderService.create(user, body));
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable String id, @RequestBody Reminder body, HttpSession session) {
    String user = currentUser(session);
    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("로그인이 필요합니다."));
    }
    Reminder updated = reminderService.update(user, id, body);
    if (updated == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("없습니다."));
    }
    return ResponseEntity.ok(updated);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable String id, HttpSession session) {
    String user = currentUser(session);
    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("로그인이 필요합니다."));
    }
    if (!reminderService.delete(user, id)) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("없습니다."));
    }
    Map<String, Object> ok = new HashMap<>();
    ok.put("ok", true);
    return ResponseEntity.ok(ok);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<?> badRequest(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(error(e.getMessage()));
  }

  private String currentUser(HttpSession session) {
    Object u = session.getAttribute(AuthController.SESSION_USER);
    return u == null ? null : String.valueOf(u);
  }

  private String[] resolveRange(String from, String to, Integer year, Integer month) {
    if (StringUtils.hasText(from) && StringUtils.hasText(to)) {
      return new String[]{from.substring(0, Math.min(10, from.length())), to.substring(0, Math.min(10, to.length()))};
    }
    YearMonth ym;
    if (year != null && month != null && month >= 1 && month <= 12) {
      ym = YearMonth.of(year, month);
    } else {
      LocalDate now = LocalDate.now();
      ym = YearMonth.of(now.getYear(), now.getMonth());
    }
    return new String[]{ym.atDay(1).toString(), ym.atEndOfMonth().toString()};
  }

  private Map<String, Object> error(String message) {
    Map<String, Object> m = new HashMap<>();
    m.put("ok", false);
    m.put("message", message);
    return m;
  }
}
