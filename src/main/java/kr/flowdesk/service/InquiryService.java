package kr.flowdesk.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.flowdesk.domain.Inquiry;
import kr.flowdesk.domain.SlaResult;
import kr.flowdesk.mapper.InquiryMapper;
import kr.flowdesk.util.HtmlEscape;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문의 CRUD · 통계 · import (사용자별 소유)
 */
@Service
public class InquiryService {

  private final InquiryMapper inquiryMapper;
  private final SlaService slaService;

  public InquiryService(InquiryMapper inquiryMapper, SlaService slaService) {
    this.inquiryMapper = inquiryMapper;
    this.slaService = slaService;
  }

  public List<Inquiry> findByOwner(String createUser) {
    return inquiryMapper.selectByCreateUser(createUser);
  }

  public Inquiry findById(String id) {
    return inquiryMapper.selectById(id);
  }

  public int countByOwner(String createUser) {
    return inquiryMapper.countByCreateUser(createUser);
  }

  public int backfillOwner(String createUser) {
    return inquiryMapper.backfillCreateUser(createUser);
  }

  @Transactional
  public Inquiry create(Inquiry inquiry, String owner) {
    normalize(inquiry);
    validate(inquiry);
    if (isBlank(inquiry.getId())) {
      inquiry.setId(String.valueOf(System.currentTimeMillis()) + "-" + UUID.randomUUID().toString().substring(0, 8));
    }
    inquiry.setCreateUser(HtmlEscape.sanitizeText(owner));
    inquiryMapper.insert(inquiry);
    return inquiryMapper.selectById(inquiry.getId());
  }

  @Transactional
  public Inquiry update(String id, Inquiry inquiry, String actor, boolean admin) {
    Inquiry existing = inquiryMapper.selectById(id);
    if (existing == null) {
      throw new IllegalArgumentException("문의가 없습니다: " + id);
    }
    assertCanModify(existing, actor, admin);
    inquiry.setId(id);
    inquiry.setCreateUser(existing.getCreateUser());
    normalize(inquiry);
    validate(inquiry);
    inquiryMapper.update(inquiry);
    return inquiryMapper.selectById(id);
  }

  @Transactional
  public void delete(String id, String actor, boolean admin) {
    Inquiry existing = inquiryMapper.selectById(id);
    if (existing == null) {
      throw new IllegalArgumentException("문의가 없습니다: " + id);
    }
    assertCanModify(existing, actor, admin);
    inquiryMapper.deleteById(id);
  }

  /** CSV/브라우저 localStorage 이관 — replace=true면 해당 소유자 데이터만 교체 */
  @Transactional
  public int importAll(List<Inquiry> list, boolean replace, String owner) {
    if (isBlank(owner)) {
      throw new IllegalArgumentException("소유 사용자가 필요합니다.");
    }
    if (replace) {
      inquiryMapper.deleteByCreateUser(owner);
    }
    int count = 0;
    for (Inquiry in : list) {
      normalize(in);
      if (isBlank(in.getCustomer())) {
        continue;
      }
      if (isBlank(in.getId())) {
        in.setId(String.valueOf(System.currentTimeMillis()) + "-" + count);
      }
      in.setCreateUser(owner);
      if (!replace) {
        Inquiry existing = inquiryMapper.selectById(in.getId());
        if (existing != null) {
          if (!owner.equals(existing.getCreateUser())) {
            in.setId(String.valueOf(System.currentTimeMillis()) + "-" + count + "-n");
            inquiryMapper.insert(in);
          } else {
            inquiryMapper.update(in);
          }
          count++;
          continue;
        }
      }
      inquiryMapper.insert(in);
      count++;
    }
    return count;
  }

  public Map<String, Object> stats(String owner) {
    return slaService.buildStats(inquiryMapper.selectByCreateUser(owner));
  }

  public SlaResult evalSla(Inquiry inquiry) {
    return slaService.evalSla(inquiry);
  }

  public Map<String, Object> listWithSla(String owner) {
    List<Inquiry> list = inquiryMapper.selectByCreateUser(owner);
    Map<String, Object> body = new HashMap<>();
    body.put("owner", owner);
    body.put("list", list);
    body.put("stats", slaService.buildStats(list));

    Map<String, SlaResult> slaMap = new HashMap<>();
    for (Inquiry d : list) {
      slaMap.put(d.getId(), slaService.evalSla(d));
    }
    body.put("slaById", slaMap);
    return body;
  }

  private void assertCanModify(Inquiry existing, String actor, boolean admin) {
    if (admin) return;
    if (existing.getCreateUser() == null || !existing.getCreateUser().equals(actor)) {
      throw new IllegalArgumentException("본인 문의만 수정·삭제할 수 있습니다.");
    }
  }

  private void validate(Inquiry in) {
    if (isBlank(in.getCustomer())) {
      throw new IllegalArgumentException("고객사명을 입력해주세요.");
    }
    if (isBlank(in.getResolvedAt())) {
      throw new IllegalArgumentException("처리일시를 입력해주세요.");
    }
  }

  private void normalize(Inquiry in) {
    if (isBlank(in.getDirection())) {
      in.setDirection("인바운드");
    }
    if (in.getFirstResponseAt() == null) {
      in.setFirstResponseAt("");
    }
    if (in.getInboundAt() == null) {
      in.setInboundAt("");
    }
    if (in.getReviewer() == null) {
      in.setReviewer("");
    }
    if (in.getNote() == null) {
      in.setNote("");
    }
    if (in.getLinkedId() == null) {
      in.setLinkedId("");
    }
    if ("문의".equals(in.getCategory())) {
      in.setCategory("일반 문의");
    }
    in.setCustomer(HtmlEscape.sanitizeText(in.getCustomer()));
    in.setReviewer(HtmlEscape.sanitizeText(in.getReviewer()));
    in.setNote(HtmlEscape.sanitizeText(in.getNote()));
    in.setCategory(HtmlEscape.sanitizeText(in.getCategory()));
    in.setModule(HtmlEscape.sanitizeText(in.getModule()));
    in.setChannel(HtmlEscape.sanitizeText(in.getChannel()));
    in.setStatus(HtmlEscape.sanitizeText(in.getStatus()));
    in.setDirection(HtmlEscape.sanitizeText(in.getDirection()));
    in.setLinkedId(HtmlEscape.sanitizeText(in.getLinkedId()));
  }

  private boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }
}
