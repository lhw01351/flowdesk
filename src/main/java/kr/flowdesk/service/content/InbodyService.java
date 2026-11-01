package kr.flowdesk.service.content;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import kr.flowdesk.domain.InbodyRecord;
import kr.flowdesk.mapper.InbodyMapper;
import kr.flowdesk.util.HtmlEscape;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InbodyService {

  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final InbodyMapper inbodyMapper;

  public InbodyService(InbodyMapper inbodyMapper) {
    this.inbodyMapper = inbodyMapper;
  }

  public List<InbodyRecord> findByOwner(String createUser) {
    return inbodyMapper.selectByCreateUser(createUser);
  }

  public InbodyRecord findById(String id) {
    return inbodyMapper.selectById(id);
  }

  public InbodyRecord findLatest(String createUser) {
    return inbodyMapper.selectLatestByCreateUser(createUser);
  }

  public int count() {
    return inbodyMapper.countAll();
  }

  public int countByOwner(String createUser) {
    return inbodyMapper.countByCreateUser(createUser);
  }

  public int backfillOwner(String createUser) {
    return inbodyMapper.backfillCreateUser(createUser);
  }

  @Transactional
  public InbodyRecord create(InbodyRecord record, String actorUsername) {
    if (record.getId() == null || record.getId().trim().isEmpty()) {
      record.setId("ib-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    }
    String now = LocalDateTime.now().format(FMT);
    String owner = record.getCreateUser();
    if (owner == null || owner.trim().isEmpty()) {
      owner = actorUsername;
    }
    record.setCreateUser(HtmlEscape.sanitizeText(owner));
    record.setCreateDt(now);
    record.setModifyDt(now);
    normalize(record);
    inbodyMapper.insert(record);
    return inbodyMapper.selectById(record.getId());
  }

  @Transactional
  public InbodyRecord update(String id, InbodyRecord record, String actorUsername, boolean admin) {
    InbodyRecord existing = inbodyMapper.selectById(id);
    if (existing == null) {
      throw new IllegalArgumentException("기록을 찾을 수 없습니다: " + id);
    }
    assertCanModify(existing, actorUsername, admin);
    record.setId(id);
    record.setCreateUser(existing.getCreateUser());
    record.setCreateDt(existing.getCreateDt());
    record.setModifyDt(LocalDateTime.now().format(FMT));
    normalize(record);
    inbodyMapper.update(record);
    return inbodyMapper.selectById(id);
  }

  @Transactional
  public void delete(String id, String actorUsername, boolean admin) {
    InbodyRecord existing = inbodyMapper.selectById(id);
    if (existing == null) {
      throw new IllegalArgumentException("기록을 찾을 수 없습니다: " + id);
    }
    assertCanModify(existing, actorUsername, admin);
    inbodyMapper.deleteById(id);
  }

  private void assertCanModify(InbodyRecord existing, String actorUsername, boolean admin) {
    if (admin) return;
    if (existing.getCreateUser() == null || !existing.getCreateUser().equals(actorUsername)) {
      throw new IllegalArgumentException("본인 인바디 기록만 수정·삭제할 수 있습니다.");
    }
  }

  private void normalize(InbodyRecord r) {
    if (r.getMeasuredAt() != null) {
      r.setMeasuredAt(HtmlEscape.sanitizeText(r.getMeasuredAt()));
    }
    if (r.getGender() != null) {
      r.setGender(HtmlEscape.sanitizeText(r.getGender()));
    }
    if (r.getNote() != null) {
      r.setNote(HtmlEscape.sanitizeText(r.getNote()));
    }
  }
}
