package kr.flowdesk.domain;

/**
 * 앱 로그인 사용자
 */
public class AppUser {

  private String id;
  private String username;
  private String password;
  private String role;
  private String authCd;
  private String authNm;
  private String displayName;
  private String createdAt;
  private String note;
  /** 저장된 프로필 이미지 파일명 (디렉터리 내) */
  private String profileImage;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }

  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }

  public String getAuthCd() { return authCd; }
  public void setAuthCd(String authCd) { this.authCd = authCd; }

  public String getAuthNm() { return authNm; }
  public void setAuthNm(String authNm) { this.authNm = authNm; }

  public String getDisplayName() { return displayName; }
  public void setDisplayName(String displayName) { this.displayName = displayName; }

  public String getCreatedAt() { return createdAt; }
  public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

  public String getNote() { return note; }
  public void setNote(String note) { this.note = note; }

  public String getProfileImage() { return profileImage; }
  public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
}
