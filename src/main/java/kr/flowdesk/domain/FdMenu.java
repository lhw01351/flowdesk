package kr.flowdesk.domain;

import java.util.ArrayList;
import java.util.List;

/** 메뉴 관리 (ispark com_pgm 참고) */
public class FdMenu {

  private String menuId;
  private String menuKey;
  private String menuNm;
  private String pageUrl;
  private String apiPrefix;
  private String upMenuId;
  private Integer menuLevel;
  private Integer sortOrder;
  private Integer realOrder;
  private String isLeaf;
  private String useYn;
  private String restrictedYn;
  private String icon;
  private String linkUrl;
  private String newUrl;
  private String path;
  private String sourceType;
  private String guideComment;
  private String content;
  private String param;
  private String manualUrl;
  private String createDt;
  private String updateDt;
  private String deleteDt;
  private String note;

  /** 권한 코드 목록 (메뉴관리 UI용) */
  private List<String> authCds = new ArrayList<>();

  public String getMenuId() { return menuId; }
  public void setMenuId(String menuId) { this.menuId = menuId; }

  public String getMenuKey() { return menuKey; }
  public void setMenuKey(String menuKey) { this.menuKey = menuKey; }

  public String getMenuNm() { return menuNm; }
  public void setMenuNm(String menuNm) { this.menuNm = menuNm; }

  public String getPageUrl() { return pageUrl; }
  public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }

  public String getApiPrefix() { return apiPrefix; }
  public void setApiPrefix(String apiPrefix) { this.apiPrefix = apiPrefix; }

  public String getUpMenuId() { return upMenuId; }
  public void setUpMenuId(String upMenuId) { this.upMenuId = upMenuId; }

  public Integer getMenuLevel() { return menuLevel; }
  public void setMenuLevel(Integer menuLevel) { this.menuLevel = menuLevel; }

  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

  public Integer getRealOrder() { return realOrder; }
  public void setRealOrder(Integer realOrder) { this.realOrder = realOrder; }

  public String getIsLeaf() { return isLeaf; }
  public void setIsLeaf(String isLeaf) { this.isLeaf = isLeaf; }

  public String getUseYn() { return useYn; }
  public void setUseYn(String useYn) { this.useYn = useYn; }

  public String getRestrictedYn() { return restrictedYn; }
  public void setRestrictedYn(String restrictedYn) { this.restrictedYn = restrictedYn; }

  public String getIcon() { return icon; }
  public void setIcon(String icon) { this.icon = icon; }

  public String getLinkUrl() { return linkUrl; }
  public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }

  public String getNewUrl() { return newUrl; }
  public void setNewUrl(String newUrl) { this.newUrl = newUrl; }

  public String getPath() { return path; }
  public void setPath(String path) { this.path = path; }

  public String getSourceType() { return sourceType; }
  public void setSourceType(String sourceType) { this.sourceType = sourceType; }

  public String getGuideComment() { return guideComment; }
  public void setGuideComment(String guideComment) { this.guideComment = guideComment; }

  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }

  public String getParam() { return param; }
  public void setParam(String param) { this.param = param; }

  public String getManualUrl() { return manualUrl; }
  public void setManualUrl(String manualUrl) { this.manualUrl = manualUrl; }

  public String getCreateDt() { return createDt; }
  public void setCreateDt(String createDt) { this.createDt = createDt; }

  public String getUpdateDt() { return updateDt; }
  public void setUpdateDt(String updateDt) { this.updateDt = updateDt; }

  public String getDeleteDt() { return deleteDt; }
  public void setDeleteDt(String deleteDt) { this.deleteDt = deleteDt; }

  public String getNote() { return note; }
  public void setNote(String note) { this.note = note; }

  public List<String> getAuthCds() { return authCds; }
  public void setAuthCds(List<String> authCds) { this.authCds = authCds == null ? new ArrayList<>() : authCds; }
}
