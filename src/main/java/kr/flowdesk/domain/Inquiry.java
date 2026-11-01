package kr.flowdesk.domain;

/**
 * 문의 1건 — 기존 HTML localStorage 항목과 필드명 동일
 */
public class Inquiry {

  private String id;
  private String direction;
  private String channel;
  private String status;
  private String category;
  private String module;
  private String inboundAt;
  private String firstResponseAt;
  private String resolvedAt;
  private String customer;
  private String reviewer;
  private String note;
  private String linkedId;
  /** 소유 사용자 (app_users.username) */
  private String createUser;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getDirection() { return direction; }
  public void setDirection(String direction) { this.direction = direction; }

  public String getChannel() { return channel; }
  public void setChannel(String channel) { this.channel = channel; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }

  public String getModule() { return module; }
  public void setModule(String module) { this.module = module; }

  public String getInboundAt() { return inboundAt; }
  public void setInboundAt(String inboundAt) { this.inboundAt = inboundAt; }

  public String getFirstResponseAt() { return firstResponseAt; }
  public void setFirstResponseAt(String firstResponseAt) { this.firstResponseAt = firstResponseAt; }

  public String getResolvedAt() { return resolvedAt; }
  public void setResolvedAt(String resolvedAt) { this.resolvedAt = resolvedAt; }

  public String getCustomer() { return customer; }
  public void setCustomer(String customer) { this.customer = customer; }

  public String getReviewer() { return reviewer; }
  public void setReviewer(String reviewer) { this.reviewer = reviewer; }

  public String getNote() { return note; }
  public void setNote(String note) { this.note = note; }

  public String getLinkedId() { return linkedId; }
  public void setLinkedId(String linkedId) { this.linkedId = linkedId; }

  public String getCreateUser() { return createUser; }
  public void setCreateUser(String createUser) { this.createUser = createUser; }
}
