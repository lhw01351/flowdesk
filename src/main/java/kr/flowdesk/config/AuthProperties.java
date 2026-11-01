package kr.flowdesk.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 로그인 계정 목록 (application.yml 의 cx.auth)
 */
@ConfigurationProperties(prefix = "cx.auth")
public class AuthProperties {

  private List<AuthUser> users = new ArrayList<>();

  public List<AuthUser> getUsers() { return users; }
  public void setUsers(List<AuthUser> users) { this.users = users; }

  public AuthUser findByUsername(String username) {
    if (username == null || users == null) {
      return null;
    }
    for (AuthUser u : users) {
      if (u != null && username.equals(u.getUsername())) {
        return u;
      }
    }
    return null;
  }
}
