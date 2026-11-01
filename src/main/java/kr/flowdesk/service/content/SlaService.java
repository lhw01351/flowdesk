package kr.flowdesk.service.content;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.flowdesk.domain.Inquiry;
import kr.flowdesk.domain.SlaResult;
import org.springframework.stereotype.Service;

/**
 * 영업시간·SLA 판정 — 기존 HTML JS(bizHoursBetween / evalSla)와 동일 규칙
 */
@Service
public class SlaService {

  private static final double BIZ_START = 9;
  private static final double BIZ_END = 18;
  private static final double LUNCH_START = 11.5;
  private static final double LUNCH_END = 12.5;
  private static final double BIZ_DAY_HOURS = (BIZ_END - BIZ_START) - (LUNCH_END - LUNCH_START);

  private static final DateTimeFormatter DT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
  private static final DateTimeFormatter DT_SEC =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  private static final Set<String> HOLIDAYS = new HashSet<>(Arrays.asList(
      "2026-01-01", "2026-02-16", "2026-02-17", "2026-02-18",
      "2026-03-01", "2026-03-02", "2026-04-30",
      "2026-05-01", "2026-05-04", "2026-05-05", "2026-05-25",
      "2026-06-03", "2026-08-17",
      "2026-09-24", "2026-09-25",
      "2026-10-05", "2026-10-09", "2026-12-25",
      "2027-01-01", "2027-02-05", "2027-02-06", "2027-02-07", "2027-02-08",
      "2027-03-01", "2027-05-05", "2027-05-13",
      "2027-06-06", "2027-08-15", "2027-08-16",
      "2027-10-03", "2027-10-04", "2027-10-14", "2027-10-15", "2027-10-16",
      "2027-12-25"
  ));

  /** 유형 → 목표 시간(시간) */
  private static final Map<String, Integer> SLA_HOURS = new HashMap<>();
  static {
    SLA_HOURS.put("일반 문의", 1);
    SLA_HOURS.put("컨설팅 문의", 2);
    SLA_HOURS.put("개선요청", 2);
    SLA_HOURS.put("오류", 4);
    SLA_HOURS.put("작업요청", 4);
    SLA_HOURS.put("확인요청", 4);
  }

  private static final Map<String, String> STATE_LABEL = new HashMap<>();
  static {
    STATE_LABEL.put("met", "충족");
    STATE_LABEL.put("over", "초과");
    STATE_LABEL.put("na", "판정 제외");
  }

  public SlaResult evalSla(Inquiry d) {
    SlaResult r = new SlaResult();
    Integer hours = d.getCategory() == null ? null : SLA_HOURS.get(d.getCategory());
    if (hours == null) {
      r.setState("na");
      r.setLabel(STATE_LABEL.get("na"));
      r.setReason("목표 미정의 유형");
      return r;
    }
    if (isBlank(d.getInboundAt())) {
      r.setState("na");
      r.setLabel(STATE_LABEL.get("na"));
      r.setReason("인입일시 없음");
      return r;
    }

    String at = "";
    String basis = "";
    if (!isBlank(d.getFirstResponseAt())) {
      at = d.getFirstResponseAt();
      basis = "1차 응대";
    } else if (!isBlank(d.getResolvedAt())) {
      at = d.getResolvedAt();
      basis = "처리";
    }
    if (isBlank(at)) {
      r.setState("na");
      r.setLabel(STATE_LABEL.get("na"));
      r.setReason("판정 시각 없음");
      return r;
    }

    Double bizH = bizHoursBetween(d.getInboundAt(), at);
    if (bizH == null) {
      r.setState("na");
      r.setLabel(STATE_LABEL.get("na"));
      r.setReason("시각 파싱 실패");
      return r;
    }

    int mins = (int) Math.round(bizH * 60);
    int targetMins = hours * 60;
    String state = mins <= targetMins ? "met" : "over";
    r.setState(state);
    r.setLabel(STATE_LABEL.get(state));
    r.setMins(mins);
    r.setOverMins(Math.max(0, mins - targetMins));
    r.setTargetHours(hours);
    r.setBasis(basis);
    r.setAt(at);
    return r;
  }

  /** 영업시간(시간) — JS bizHoursBetween과 동일 */
  public Double bizHoursBetween(String startStr, String endStr) {
    if (isBlank(startStr) || isBlank(endStr)) {
      return null;
    }
    LocalDateTime s = parseDateTime(startStr);
    LocalDateTime e = parseDateTime(endStr);
    if (s == null || e == null) {
      return null;
    }
    if (!e.isAfter(s)) {
      return 0.0;
    }
    boolean sameDay = s.toLocalDate().equals(e.toLocalDate());
    if (sameDay) {
      if (!isBizDay(s.toLocalDate())) {
        return 0.0;
      }
      return Math.max(0, bizHoursInDay(e) - bizHoursInDay(s));
    }
    double total = 0;
    if (isBizDay(s.toLocalDate())) {
      total += BIZ_DAY_HOURS - bizHoursInDay(s);
    }
    LocalDate cur = s.toLocalDate().plusDays(1);
    LocalDate endDay = e.toLocalDate();
    while (cur.isBefore(endDay)) {
      if (isBizDay(cur)) {
        total += BIZ_DAY_HOURS;
      }
      cur = cur.plusDays(1);
    }
    if (isBizDay(e.toLocalDate())) {
      total += bizHoursInDay(e);
    }
    return Math.max(0, total);
  }

  /** 달력 기준 영업일 시간(시간) — OKR 1영업일 판정용 */
  public Double calendarBizDaysBetween(String startStr, String endStr) {
    if (isBlank(startStr) || isBlank(endStr)) {
      return null;
    }
    LocalDateTime s = parseDateTime(startStr);
    LocalDateTime e = parseDateTime(endStr);
    if (s == null || e == null) {
      return null;
    }
    if (!e.isAfter(s)) {
      return 0.0;
    }
    double diffH = java.time.Duration.between(s, e).toMinutes() / 60.0;
    double nonBizH = 0;
    LocalDate cur = s.toLocalDate();
    LocalDate endDay = e.toLocalDate();
    while (!cur.isAfter(endDay)) {
      if (!isBizDay(cur)) {
        nonBizH += 24;
      }
      cur = cur.plusDays(1);
    }
    return Math.max(0, diffH - nonBizH);
  }

  public Map<String, Object> buildStats(List<Inquiry> list) {
    Map<String, Object> stats = new HashMap<>();
    int met = 0, over = 0, na = 0;
    Map<String, Integer> byCategory = new HashMap<>();
    Map<String, Integer> byModule = new HashMap<>();
    Map<String, Integer> byChannel = new HashMap<>();
    int okrWithin = 0, okrTotal = 0;

    for (Inquiry d : list) {
      SlaResult sla = evalSla(d);
      if ("met".equals(sla.getState())) {
        met++;
      } else if ("over".equals(sla.getState())) {
        over++;
      } else {
        na++;
      }

      String cat = nullToEmpty(d.getCategory());
      String mod = nullToEmpty(d.getModule());
      String ch = nullToEmpty(d.getChannel());
      byCategory.merge(cat, 1, Integer::sum);
      byModule.merge(mod, 1, Integer::sum);
      byChannel.merge(ch, 1, Integer::sum);

      // OKR: 인입→1차응대(없으면 처리) 1영업일(24h 영업일 환산) 이내
      if (!isBlank(d.getInboundAt())) {
        String resp = !isBlank(d.getFirstResponseAt()) ? d.getFirstResponseAt() : d.getResolvedAt();
        if (!isBlank(resp)) {
          Double h = calendarBizDaysBetween(d.getInboundAt(), resp);
          if (h != null) {
            okrTotal++;
            if (h <= 24) {
              okrWithin++;
            }
          }
        }
      }
    }

    stats.put("total", list.size());
    stats.put("slaMet", met);
    stats.put("slaOver", over);
    stats.put("slaNa", na);
    stats.put("byCategory", byCategory);
    stats.put("byModule", byModule);
    stats.put("byChannel", byChannel);
    stats.put("okrWithin", okrWithin);
    stats.put("okrTotal", okrTotal);
    stats.put("okrRate", okrTotal > 0 ? Math.round(okrWithin * 1000.0 / okrTotal) / 10.0 : 0);
    return stats;
  }

  private double bizHoursInDay(LocalDateTime d) {
    double h = d.getHour() + d.getMinute() / 60.0;
    double worked = Math.min(Math.max(h, BIZ_START), BIZ_END) - BIZ_START;
    double lunch = Math.min(Math.max(h, LUNCH_START), LUNCH_END) - LUNCH_START;
    return worked - lunch;
  }

  private boolean isBizDay(LocalDate d) {
    int dow = d.getDayOfWeek().getValue(); // 1=월 … 7=일
    if (dow == 6 || dow == 7) {
      return false;
    }
    String key = String.format("%04d-%02d-%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    return !HOLIDAYS.contains(key);
  }

  private LocalDateTime parseDateTime(String s) {
    String v = s.trim();
    try {
      if (v.length() == 16) {
        return LocalDateTime.parse(v, DT);
      }
      if (v.length() >= 19) {
        return LocalDateTime.parse(v.substring(0, 19), DT_SEC);
      }
      return LocalDateTime.parse(v, DT);
    } catch (DateTimeParseException e) {
      try {
        return LocalDateTime.parse(v.replace(" ", "T").substring(0, Math.min(16, v.length())), DT);
      } catch (Exception ex) {
        return null;
      }
    }
  }

  private boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  private String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
