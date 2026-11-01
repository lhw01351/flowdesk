package kr.flowdesk.domain;

import java.util.ArrayList;
import java.util.List;

/** 홈 실시간 채팅 메시지 */
public class ChatMessage {

  private String id;
  private String username;
  private String displayName;
  private String bodyText;
  private String createdAt;
  private List<ChatAttachment> attachments = new ArrayList<>();

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getDisplayName() { return displayName; }
  public void setDisplayName(String displayName) { this.displayName = displayName; }

  public String getBodyText() { return bodyText; }
  public void setBodyText(String bodyText) { this.bodyText = bodyText; }

  public String getCreatedAt() { return createdAt; }
  public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

  public List<ChatAttachment> getAttachments() { return attachments; }
  public void setAttachments(List<ChatAttachment> attachments) {
    this.attachments = attachments != null ? attachments : new ArrayList<>();
  }
}
