package kr.flowdesk.web.system;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import kr.flowdesk.domain.AccessLog;
import kr.flowdesk.service.system.AccessLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access-logs")
public class AccessLogController {

  private final AccessLogService accessLogService;

  public AccessLogController(AccessLogService accessLogService) {
    this.accessLogService = accessLogService;
  }

  @GetMapping
  public List<AccessLog> list(
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(required = false) String user,
      @RequestParam(required = false) String eventType) {
    return accessLogService.findFiltered(from, to, user, eventType);
  }

  /** 로그인 후 페이지 접속 / GPS 포함 기록 */
  @PostMapping
  public AccessLog create(@RequestBody Map<String, Object> body, HttpServletRequest request, HttpSession session) {
    Object userObj = session.getAttribute(AuthController.SESSION_USER);
    String username = userObj == null ? null : String.valueOf(userObj);
    String eventType = str(body.get("eventType"));
    String pagePath = str(body.get("pagePath"));
    String menuNm = str(body.get("menuNm"));
    Double lat = toDouble(body.get("latitude"));
    Double lng = toDouble(body.get("longitude"));
    Double acc = toDouble(body.get("accuracy"));
    String gpsStatus = str(body.get("gpsStatus"));
    return accessLogService.record(
        username, eventType, pagePath, menuNm, lat, lng, acc, gpsStatus, request);
  }

  @GetMapping("/summary")
  public Map<String, Object> summary(
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(required = false) String user,
      @RequestParam(required = false) String eventType) {
    List<AccessLog> list = accessLogService.findFiltered(from, to, user, eventType);
    Map<String, Object> res = new HashMap<>();
    res.put("count", list.size());
    res.put("list", list);
    return res;
  }

  /** 선택 로그 삭제 */
  @PostMapping("/delete")
  public Map<String, Object> deleteSelected(@RequestBody(required = false) Map<String, Object> body) {
    List<String> ids = new java.util.ArrayList<>();
    Object raw = body == null ? null : body.get("ids");
    if (raw instanceof List) {
      for (Object o : (List<?>) raw) {
        if (o != null) ids.add(String.valueOf(o));
      }
    }
    int deleted = accessLogService.deleteByIds(ids);
    Map<String, Object> res = new HashMap<>();
    res.put("deleted", deleted);
    return res;
  }

  /** 전체 로그 삭제 */
  @PostMapping("/delete-all")
  public Map<String, Object> deleteAll() {
    int deleted = accessLogService.deleteAll();
    Map<String, Object> res = new HashMap<>();
    res.put("deleted", deleted);
    return res;
  }

  private String str(Object o) {
    return o == null ? null : String.valueOf(o).trim();
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
