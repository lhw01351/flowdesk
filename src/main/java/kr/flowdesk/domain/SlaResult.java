package kr.flowdesk.domain;

/**
 * SLA 판정 결과 (충족/초과/판정제외)
 */
public class SlaResult {

  private String state;   // met | over | na
  private String label;   // 충족 | 초과 | 판정 제외
  private Integer mins;
  private Integer overMins;
  private Integer targetHours;
  private String basis;
  private String reason;
  private String at;

  public String getState() { return state; }
  public void setState(String state) { this.state = state; }

  public String getLabel() { return label; }
  public void setLabel(String label) { this.label = label; }

  public Integer getMins() { return mins; }
  public void setMins(Integer mins) { this.mins = mins; }

  public Integer getOverMins() { return overMins; }
  public void setOverMins(Integer overMins) { this.overMins = overMins; }

  public Integer getTargetHours() { return targetHours; }
  public void setTargetHours(Integer targetHours) { this.targetHours = targetHours; }

  public String getBasis() { return basis; }
  public void setBasis(String basis) { this.basis = basis; }

  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }

  public String getAt() { return at; }
  public void setAt(String at) { this.at = at; }
}
