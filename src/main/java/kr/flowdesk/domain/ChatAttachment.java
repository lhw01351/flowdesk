package kr.flowdesk.domain;

/** 채팅 첨부파일 메타 */
public class ChatAttachment {

  private String id;
  private String messageId;
  private String originalName;
  private String storedName;
  private String contentType;
  private Long sizeBytes;
  private String uploadedBy;
  private String uploadedAt;
  private String url;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getMessageId() { return messageId; }
  public void setMessageId(String messageId) { this.messageId = messageId; }

  public String getOriginalName() { return originalName; }
  public void setOriginalName(String originalName) { this.originalName = originalName; }

  public String getStoredName() { return storedName; }
  public void setStoredName(String storedName) { this.storedName = storedName; }

  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }

  public Long getSizeBytes() { return sizeBytes; }
  public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

  public String getUploadedBy() { return uploadedBy; }
  public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

  public String getUploadedAt() { return uploadedAt; }
  public void setUploadedAt(String uploadedAt) { this.uploadedAt = uploadedAt; }

  public String getUrl() { return url; }
  public void setUrl(String url) { this.url = url; }
}
