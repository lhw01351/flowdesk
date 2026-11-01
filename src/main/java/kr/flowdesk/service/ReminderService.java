package kr.flowdesk.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import kr.flowdesk.domain.Reminder;
import kr.flowdesk.mapper.ReminderMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReminderService {

  private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final ReminderMapper reminderMapper;

  public ReminderService(ReminderMapper reminderMapper) {
    this.reminderMapper = reminderMapper;
  }

  public List<Reminder> findRange(String username, String from, String to) {
    return reminderMapper.selectByUserAndRange(username, from, to);
  }

  public Reminder findById(String id) {
    return reminderMapper.selectById(id);
  }

  public Reminder create(String username, Reminder body) {
    Reminder row = new Reminder();
    row.setId(UUID.randomUUID().toString().replace("-", ""));
    row.setUsername(username);
    row.setRemindDate(requireDate(body.getRemindDate()));
    row.setTitle(requireTitle(body.getTitle()));
    row.setContent(body.getContent());
    row.setDoneYn(normalizeDone(body.getDoneYn()));
    String now = LocalDateTime.now().format(DT);
    row.setCreateDt(now);
    row.setModifyDt(now);
    reminderMapper.insert(row);
    return row;
  }

  public Reminder update(String username, String id, Reminder body) {
    Reminder existing = reminderMapper.selectById(id);
    if (existing == null || !username.equals(existing.getUsername())) {
      return null;
    }
    existing.setRemindDate(requireDate(body.getRemindDate()));
    existing.setTitle(requireTitle(body.getTitle()));
    existing.setContent(body.getContent());
    existing.setDoneYn(normalizeDone(body.getDoneYn()));
    existing.setModifyDt(LocalDateTime.now().format(DT));
    reminderMapper.update(existing);
    return existing;
  }

  public boolean delete(String username, String id) {
    return reminderMapper.delete(id, username) > 0;
  }

  private String requireDate(String date) {
    if (!StringUtils.hasText(date) || date.trim().length() < 10) {
      throw new IllegalArgumentException("날짜를 입력하세요.");
    }
    return date.trim().substring(0, 10);
  }

  private String requireTitle(String title) {
    if (!StringUtils.hasText(title)) {
      throw new IllegalArgumentException("제목을 입력하세요.");
    }
    String t = title.trim();
    if (t.length() > 200) t = t.substring(0, 200);
    return t;
  }

  private String normalizeDone(String doneYn) {
    return "Y".equalsIgnoreCase(doneYn) ? "Y" : "N";
  }
}
