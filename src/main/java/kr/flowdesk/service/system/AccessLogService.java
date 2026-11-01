package kr.flowdesk.service.system;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import kr.flowdesk.domain.AccessLog;
import kr.flowdesk.domain.FdMenu;
import kr.flowdesk.mapper.AccessLogMapper;
import kr.flowdesk.mapper.FdMenuMapper;
import kr.flowdesk.util.HtmlEscape;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessLogService {

  private static final DateTimeFormatter AT_MIN =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final AccessLogMapper accessLogMapper;
  private final FdMenuMapper fdMenuMapper;

  public AccessLogService(AccessLogMapper accessLogMapper, FdMenuMapper fdMenuMapper) {
    this.accessLogMapper = accessLogMapper;
    this.fdMenuMapper = fdMenuMapper;
  }

  public List<AccessLog> findAll() {
    return accessLogMapper.selectAll();
  }

  /**
   * from/to 는 yyyy-MM-dd (to 포함 → 다음날 00:00 미만)
   */
  public List<AccessLog> findByRange(String from, String to) {
    return findFiltered(from, to, null, null);
  }

  public List<AccessLog> findFiltered(String from, String to, String username, String eventType) {
    String fromTs = null;
    String toExclusive = null;
    if (from != null && !from.isEmpty() && to != null && !to.isEmpty()) {
      fromTs = from + " 00:00";
      LocalDate toDate = LocalDate.parse(to).plusDays(1);
      toExclusive = toDate + " 00:00";
    }
    String user = blankToNull(username);
    String ev = blankToNull(eventType);
    if (fromTs == null && user == null && ev == null) {
      return findAll();
    }
    return accessLogMapper.selectByFilter(fromTs, toExclusive, user, ev);
  }

  @Transactional
  public AccessLog record(
      String username,
      String eventType,
      String pagePath,
      Double latitude,
      Double longitude,
      Double accuracy,
      String gpsStatus,
      HttpServletRequest request) {
    return record(username, eventType, pagePath, null, latitude, longitude, accuracy, gpsStatus, request);
  }

  @Transactional
  public AccessLog record(
      String username,
      String eventType,
      String pagePath,
      String menuNm,
      Double latitude,
      Double longitude,
      Double accuracy,
      String gpsStatus,
      HttpServletRequest request) {

    AccessLog log = new AccessLog();
    log.setId("al-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    log.setAccessedAt(LocalDateTime.now().format(AT_MIN));
    log.setUsername(HtmlEscape.sanitizeText(username));
    log.setEventType(HtmlEscape.sanitizeText(eventType == null ? "PAGE" : eventType));
    log.setPagePath(HtmlEscape.sanitizeText(pagePath));
    String resolvedMenu = blankToNull(menuNm);
    if (resolvedMenu == null) {
      resolvedMenu = resolveMenuNm(pagePath, eventType);
    }
    log.setMenuNm(HtmlEscape.sanitizeText(resolvedMenu));
    String ua = request == null ? null : request.getHeader("User-Agent");
    log.setUserAgent(truncate(HtmlEscape.sanitizeText(ua), 900));
    log.setDeviceType(detectDevice(ua));
    log.setIpAddress(clientIp(request));
    log.setLatitude(latitude);
    log.setLongitude(longitude);
    log.setGpsAccuracyM(accuracy);
    if (gpsStatus != null && !gpsStatus.isEmpty()) {
      log.setGpsStatus(HtmlEscape.sanitizeText(gpsStatus));
    } else if (latitude != null && longitude != null) {
      log.setGpsStatus("OK");
    } else {
      log.setGpsStatus("없음");
    }
    accessLogMapper.insert(log);
    return log;
  }

  public String resolveMenuNm(String pagePath, String eventType) {
    if (eventType != null) {
      String ev = eventType.trim().toUpperCase();
      if ("LOGIN".equals(ev)) return "로그인";
      if ("LOGOUT".equals(ev)) return "로그아웃";
      if ("LOGIN_AS".equals(ev)) return "대리 로그인";
    }
    String file = pageFile(pagePath);
    if (file.isEmpty()) return null;
    if ("login.html".equalsIgnoreCase(file)) return "로그인";
    try {
      for (FdMenu m : fdMenuMapper.selectAll()) {
        if (m == null || m.getPageUrl() == null || m.getPageUrl().trim().isEmpty()) continue;
        if (pageFile(m.getPageUrl()).equalsIgnoreCase(file)) {
          return m.getMenuNm();
        }
      }
    } catch (Exception ignored) {
    }
    return file;
  }

  private String pageFile(String pagePath) {
    if (pagePath == null || pagePath.trim().isEmpty()) return "";
    String p = pagePath.trim().replace('\\', '/');
    int q = p.indexOf('?');
    if (q >= 0) p = p.substring(0, q);
    int slash = p.lastIndexOf('/');
    return slash >= 0 ? p.substring(slash + 1) : p;
  }

  public static String detectDevice(String ua) {
    if (ua == null || ua.isEmpty()) {
      return "알수없음";
    }
    String u = ua.toLowerCase();
    if (u.contains("iphone") || u.contains("ipod")) {
      return "iPhone";
    }
    if (u.contains("ipad")) {
      return "iPad";
    }
    if (u.contains("android")) {
      return "Android";
    }
    if (u.contains("mobile") || u.contains("opera mini") || u.contains("iemobile")) {
      return "Mobile";
    }
    return "PC";
  }

  private String clientIp(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isEmpty()) {
      return xff.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private String truncate(String s, int max) {
    if (s == null) return null;
    return s.length() <= max ? s : s.substring(0, max);
  }

  @Transactional
  public int deleteByIds(List<String> ids) {
    if (ids == null || ids.isEmpty()) return 0;
    Set<String> uniq = new LinkedHashSet<>();
    for (String id : ids) {
      if (id == null) continue;
      String t = id.trim();
      if (!t.isEmpty()) uniq.add(t);
    }
    if (uniq.isEmpty()) return 0;
    return accessLogMapper.deleteByIds(new ArrayList<>(uniq));
  }

  @Transactional
  public int deleteAll() {
    return accessLogMapper.deleteAll();
  }

  public int countAll() {
    return accessLogMapper.countAll();
  }

  private String blankToNull(String s) {
    if (s == null || s.trim().isEmpty()) return null;
    return s.trim();
  }
}
