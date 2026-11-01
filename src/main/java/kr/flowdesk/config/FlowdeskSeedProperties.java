package kr.flowdesk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 시드 계정 비밀번호 — 환경변수로만 주입 (저장소에 평문 비번 금지).
 * FLOWDESK_PASS_ADMIN / HWLEE / HANI / GUEST
 */
@ConfigurationProperties(prefix = "flowdesk.seed")
public class FlowdeskSeedProperties {

  private String adminPassword = "";
  private String hwleePassword = "";
  private String haniPassword = "";
  /** 포폴 게스트 기본값 guest (공개 데모용) */
  private String guestPassword = "guest";

  public String getAdminPassword() { return adminPassword; }
  public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }

  public String getHwleePassword() { return hwleePassword; }
  public void setHwleePassword(String hwleePassword) { this.hwleePassword = hwleePassword; }

  public String getHaniPassword() { return haniPassword; }
  public void setHaniPassword(String haniPassword) { this.haniPassword = haniPassword; }

  public String getGuestPassword() { return guestPassword; }
  public void setGuestPassword(String guestPassword) { this.guestPassword = guestPassword; }
}
