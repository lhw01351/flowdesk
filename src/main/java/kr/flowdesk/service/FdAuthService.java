package kr.flowdesk.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import kr.flowdesk.domain.FdAuth;
import kr.flowdesk.mapper.FdAuthMapper;
import kr.flowdesk.util.HtmlEscape;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FdAuthService {

  public static final String AUTH_ADMIN = "01";
  public static final String AUTH_USER = "02";

  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final FdAuthMapper fdAuthMapper;

  public FdAuthService(FdAuthMapper fdAuthMapper) {
    this.fdAuthMapper = fdAuthMapper;
  }

  public List<FdAuth> findAll() {
    return fdAuthMapper.selectAll();
  }

  public FdAuth findByCd(String authCd) {
    return fdAuthMapper.selectByCd(authCd);
  }

  public int count() {
    return fdAuthMapper.countAll();
  }

  @Transactional
  public FdAuth create(FdAuth auth) {
    String cd = auth.getAuthCd() == null ? "" : auth.getAuthCd().trim();
    String nm = auth.getAuthNm() == null ? "" : auth.getAuthNm().trim();
    if (cd.isEmpty()) throw new IllegalArgumentException("권한코드를 입력해주세요.");
    if (nm.isEmpty()) throw new IllegalArgumentException("권한명을 입력해주세요.");
    if (fdAuthMapper.selectByCd(cd) != null) {
      throw new IllegalArgumentException("이미 존재하는 권한코드입니다.");
    }
    auth.setAuthCd(HtmlEscape.sanitizeText(cd));
    auth.setAuthNm(HtmlEscape.sanitizeText(nm));
    auth.setUseYn(auth.getUseYn() == null || auth.getUseYn().isEmpty() ? "Y" : auth.getUseYn());
    if (auth.getSortOrder() == null) auth.setSortOrder(0);
    auth.setNote(HtmlEscape.sanitizeText(auth.getNote()));
    auth.setCreateDt(LocalDateTime.now().format(FMT));
    fdAuthMapper.insert(auth);
    return fdAuthMapper.selectByCd(auth.getAuthCd());
  }

  @Transactional
  public FdAuth update(String authCd, FdAuth incoming) {
    FdAuth existing = fdAuthMapper.selectByCd(authCd);
    if (existing == null) throw new IllegalArgumentException("권한을 찾을 수 없습니다.");
    if (incoming.getAuthNm() != null) {
      existing.setAuthNm(HtmlEscape.sanitizeText(incoming.getAuthNm().trim()));
    }
    if (incoming.getSortOrder() != null) existing.setSortOrder(incoming.getSortOrder());
    if (incoming.getUseYn() != null) existing.setUseYn(incoming.getUseYn());
    if (incoming.getNote() != null) existing.setNote(HtmlEscape.sanitizeText(incoming.getNote()));
    fdAuthMapper.update(existing);
    return fdAuthMapper.selectByCd(authCd);
  }
}
