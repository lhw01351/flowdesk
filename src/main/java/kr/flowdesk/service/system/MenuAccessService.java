package kr.flowdesk.service.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpSession;
import kr.flowdesk.domain.FdMenu;
import kr.flowdesk.mapper.FdMenuMapper;
import kr.flowdesk.web.system.AuthController;
import org.springframework.stereotype.Service;

/**
 * 권한·제한메뉴 기반 메뉴/경로 접근 판단 + 대/중/소 트리
 * restricted 메뉴 · 회원/메뉴 API: 권한 01 (계정명 화이트리스트 없음)
 */
@Service
public class MenuAccessService {

  private final FdMenuMapper fdMenuMapper;

  public MenuAccessService(FdMenuMapper fdMenuMapper) {
    this.fdMenuMapper = fdMenuMapper;
  }

  /** 전체관리자(01) 여부 — 타인 데이터 조회(인바디·문의 콤보) 등 */
  public static boolean isAuthAdmin(String authCd) {
    return FdAuthService.AUTH_ADMIN.equals(authCd);
  }

  /** 회원·메뉴 관리 API/화면 — 권한 01 */
  public static boolean isManager(HttpSession session) {
    return isAuthAdmin(AuthController.authCdOf(session));
  }

  /** 권한에 매핑된 메뉴 — restricted 는 권한 01 만 */
  public List<FdMenu> menusFor(String username, String authCd) {
    if (authCd == null || authCd.trim().isEmpty()) {
      return new ArrayList<>();
    }
    String cd = authCd.trim();
    List<FdMenu> raw = fdMenuMapper.selectByAuthCd(cd);
    List<FdMenu> out = new ArrayList<>();
    boolean canRestricted = isAuthAdmin(cd);
    for (FdMenu m : raw) {
      if (m == null) continue;
      if ("Y".equalsIgnoreCase(m.getRestrictedYn()) && !canRestricted) {
        continue;
      }
      out.add(m);
    }
    return out;
  }

  /** 셸용: 리프 flags + 평탄 menuList + 계층 menuTree */
  public Map<String, Object> menuPayload(String username, String authCd) {
    List<FdMenu> allowed = menusFor(username, authCd);
    Map<String, FdMenu> allMap = new LinkedHashMap<>();
    for (FdMenu m : fdMenuMapper.selectAll()) {
      if (m != null && m.getMenuId() != null) {
        allMap.put(m.getMenuId(), m);
      }
    }

    Set<String> visibleIds = new HashSet<>();
    for (FdMenu m : allowed) {
      String id = m.getMenuId();
      while (id != null && !id.isEmpty()) {
        if (!visibleIds.add(id)) break;
        FdMenu cur = allMap.get(id);
        if (cur == null) break;
        id = cur.getUpMenuId();
      }
    }

    Map<String, Boolean> flags = new LinkedHashMap<>();
    List<Map<String, Object>> flatLeaves = new ArrayList<>();
    for (FdMenu m : allowed) {
      if (!isLeaf(m)) continue;
      String key = m.getMenuKey();
      if (key == null || key.isEmpty()) continue;
      flags.put(key, true);
      flatLeaves.add(toItem(m));
    }

    List<Map<String, Object>> tree = buildTree(allMap, visibleIds, null);

    Map<String, Object> payload = new HashMap<>();
    payload.put("menus", flags);
    payload.put("menuList", flatLeaves);
    payload.put("menuTree", tree);
    return payload;
  }

  private List<Map<String, Object>> buildTree(
      Map<String, FdMenu> allMap, Set<String> visibleIds, String parentId) {
    List<Map<String, Object>> nodes = new ArrayList<>();
    for (FdMenu m : allMap.values()) {
      if (!visibleIds.contains(m.getMenuId())) continue;
      String up = m.getUpMenuId();
      boolean isRoot = (parentId == null || parentId.isEmpty())
          && (up == null || up.trim().isEmpty());
      boolean isChild = parentId != null && parentId.equals(up);
      if (!isRoot && !isChild) continue;

      Map<String, Object> node = toItem(m);
      node.put("level", m.getMenuLevel() == null ? 1 : m.getMenuLevel());
      node.put("isLeaf", isLeaf(m));
      node.put("upMenuId", up);
      List<Map<String, Object>> children = buildTree(allMap, visibleIds, m.getMenuId());
      if (!isLeaf(m) && children.isEmpty()) {
        continue;
      }
      node.put("children", children);
      nodes.add(node);
    }
    return nodes;
  }

  private Map<String, Object> toItem(FdMenu m) {
    Map<String, Object> item = new HashMap<>();
    item.put("key", m.getMenuKey());
    item.put("name", m.getMenuNm());
    item.put("page", pageFile(m.getPageUrl()));
    item.put("icon", m.getIcon());
    item.put("menuId", m.getMenuId());
    return item;
  }

  private boolean isLeaf(FdMenu m) {
    if (m.getIsLeaf() != null && !m.getIsLeaf().isEmpty()) {
      return "Y".equalsIgnoreCase(m.getIsLeaf());
    }
    String page = m.getPageUrl();
    return page != null && !page.trim().isEmpty();
  }

  public boolean canAccessPath(String username, String authCd, String path) {
    if (path == null || path.isEmpty() || "/".equals(path) || "/home.html".equals(path)) {
      return true;
    }
    List<FdMenu> allowed = menusFor(username, authCd);
    for (FdMenu m : allowed) {
      if (!isLeaf(m)) continue;
      if (matches(m, path)) {
        return true;
      }
    }
    if ("/health.html".equals(path) || path.startsWith("/api/inbody")) {
      for (FdMenu m : allowed) {
        if ("health".equals(m.getMenuKey())) return true;
      }
    }
    if ("/remind.html".equals(path) || path.startsWith("/api/reminders")) {
      for (FdMenu m : allowed) {
        if ("remind".equals(m.getMenuKey()) || "home".equals(m.getMenuKey())) return true;
      }
    }
    return false;
  }

  private boolean matches(FdMenu m, String path) {
    String page = m.getPageUrl();
    if (page != null && !page.isEmpty()) {
      String p = page.startsWith("/") ? page : "/" + page;
      if (path.equals(p) || path.startsWith(p + "?")) {
        return true;
      }
    }
    String api = m.getApiPrefix();
    if (api != null && !api.isEmpty() && path.startsWith(api)) {
      return true;
    }
    return false;
  }

  private String pageFile(String pageUrl) {
    if (pageUrl == null || pageUrl.isEmpty()) return "";
    String p = pageUrl;
    if (p.startsWith("/")) p = p.substring(1);
    return p;
  }
}
