package kr.flowdesk.config;

/**
 * 로그인 계정 1명 (application.yml cx.auth.users)
 */
public class AuthUser {

  private String username;
  private String password;
  /** 레거시 역할(yml 폴백). 실제 권한은 app_users.auth_cd */
  private String role = "ADMIN";

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }

  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
}
