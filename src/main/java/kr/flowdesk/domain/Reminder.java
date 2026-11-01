package kr.flowdesk.domain;

/** 사용자별 리마인드 1건 */
public class Reminder {

  private String id;
  private String username;
  private String remindDate;
  private String title;
  private String content;
  private String doneYn;
  private String createDt;
  private String modifyDt;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getRemindDate() { return remindDate; }
  public void setRemindDate(String remindDate) { this.remindDate = remindDate; }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }

  public String getDoneYn() { return doneYn; }
  public void setDoneYn(String doneYn) { this.doneYn = doneYn; }

  public String getCreateDt() { return createDt; }
  public void setCreateDt(String createDt) { this.createDt = createDt; }

  public String getModifyDt() { return modifyDt; }
  public void setModifyDt(String modifyDt) { this.modifyDt = modifyDt; }
}
