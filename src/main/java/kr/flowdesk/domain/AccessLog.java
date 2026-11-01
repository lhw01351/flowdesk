package kr.flowdesk.domain;

/**
 * 접속 로그 1건
 */
public class AccessLog {

  private String id;
  private String accessedAt;
  private String username;
  private String eventType;
  private String pagePath;
  private String menuNm;
  private String deviceType;
  private String userAgent;
  private String ipAddress;
  private Double latitude;
  private Double longitude;
  private Double gpsAccuracyM;
  private String gpsStatus;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getAccessedAt() { return accessedAt; }
  public void setAccessedAt(String accessedAt) { this.accessedAt = accessedAt; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getEventType() { return eventType; }
  public void setEventType(String eventType) { this.eventType = eventType; }

  public String getPagePath() { return pagePath; }
  public void setPagePath(String pagePath) { this.pagePath = pagePath; }

  public String getMenuNm() { return menuNm; }
  public void setMenuNm(String menuNm) { this.menuNm = menuNm; }

  public String getDeviceType() { return deviceType; }
  public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

  public String getUserAgent() { return userAgent; }
  public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

  public String getIpAddress() { return ipAddress; }
  public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

  public Double getLatitude() { return latitude; }
  public void setLatitude(Double latitude) { this.latitude = latitude; }

  public Double getLongitude() { return longitude; }
  public void setLongitude(Double longitude) { this.longitude = longitude; }

  public Double getGpsAccuracyM() { return gpsAccuracyM; }
  public void setGpsAccuracyM(Double gpsAccuracyM) { this.gpsAccuracyM = gpsAccuracyM; }

  public String getGpsStatus() { return gpsStatus; }
  public void setGpsStatus(String gpsStatus) { this.gpsStatus = gpsStatus; }
}
