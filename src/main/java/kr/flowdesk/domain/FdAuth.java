package kr.flowdesk.domain;

/** 권한 마스터 */
public class FdAuth {

  private String authCd;
  private String authNm;
  private Integer sortOrder;
  private String useYn;
  private String createDt;
  private String note;

  public String getAuthCd() { return authCd; }
  public void setAuthCd(String authCd) { this.authCd = authCd; }

  public String getAuthNm() { return authNm; }
  public void setAuthNm(String authNm) { this.authNm = authNm; }

  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

  public String getUseYn() { return useYn; }
  public void setUseYn(String useYn) { this.useYn = useYn; }

  public String getCreateDt() { return createDt; }
  public void setCreateDt(String createDt) { this.createDt = createDt; }

  public String getNote() { return note; }
  public void setNote(String note) { this.note = note; }
}
