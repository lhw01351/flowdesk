package kr.flowdesk.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import kr.flowdesk.domain.FdMenu;
import kr.flowdesk.mapper.FdMenuMapper;
import kr.flowdesk.util.HtmlEscape;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FdMenuService {

  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final FdMenuMapper fdMenuMapper;

  public FdMenuService(FdMenuMapper fdMenuMapper) {
    this.fdMenuMapper = fdMenuMapper;
  }

  public List<FdMenu> findAllWithAuth() {
    List<FdMenu> list = fdMenuMapper.selectAll();
    for (FdMenu m : list) {
      m.setAuthCds(fdMenuMapper.selectAuthCdsByMenuId(m.getMenuId()));
    }
    return list;
  }

  public FdMenu findById(String menuId) {
    FdMenu m = fdMenuMapper.selectById(menuId);
    if (m != null) {
      m.setAuthCds(fdMenuMapper.selectAuthCdsByMenuId(menuId));
    }
    return m;
  }

  public int count() {
    return fdMenuMapper.countAll();
  }

  public int countMenuAuth() {
    return fdMenuMapper.countMenuAuth();
  }

  @Transactional
  public FdMenu update(String menuId, FdMenu incoming) {
    FdMenu existing = fdMenuMapper.selectById(menuId);
    if (existing == null) throw new IllegalArgumentException("메뉴를 찾을 수 없습니다.");

    if (incoming.getMenuNm() != null && !incoming.getMenuNm().trim().isEmpty()) {
      existing.setMenuNm(HtmlEscape.sanitizeText(incoming.getMenuNm().trim()));
    }
    if (incoming.getSortOrder() != null) existing.setSortOrder(incoming.getSortOrder());
    if (incoming.getRealOrder() != null) existing.setRealOrder(incoming.getRealOrder());
    if (incoming.getUseYn() != null && !incoming.getUseYn().isEmpty()) {
      existing.setUseYn(incoming.getUseYn());
    }
    if (incoming.getRestrictedYn() != null && !incoming.getRestrictedYn().isEmpty()) {
      existing.setRestrictedYn(incoming.getRestrictedYn());
    }
    if (incoming.getIcon() != null) existing.setIcon(HtmlEscape.sanitizeText(incoming.getIcon()));
    if (incoming.getGuideComment() != null) {
      existing.setGuideComment(HtmlEscape.sanitizeText(incoming.getGuideComment()));
    }
    if (incoming.getNote() != null) existing.setNote(HtmlEscape.sanitizeText(incoming.getNote()));
    if (incoming.getUpMenuId() != null) {
      String up = incoming.getUpMenuId().trim();
      existing.setUpMenuId(up.isEmpty() ? null : up);
    }
    if (incoming.getMenuLevel() != null) existing.setMenuLevel(incoming.getMenuLevel());
    if (incoming.getIsLeaf() != null && !incoming.getIsLeaf().isEmpty()) {
      existing.setIsLeaf(incoming.getIsLeaf());
    }
    existing.setUpdateDt(LocalDateTime.now().format(FMT));
    fdMenuMapper.update(existing);

    if (incoming.getAuthCds() != null) {
      replaceAuth(menuId, incoming.getAuthCds());
    }
    return findById(menuId);
  }

  @Transactional
  public void replaceAuth(String menuId, List<String> authCds) {
    fdMenuMapper.deleteAuthByMenuId(menuId);
    String now = LocalDateTime.now().format(FMT);
    if (authCds == null) return;
    for (String cd : authCds) {
      if (cd == null || cd.trim().isEmpty()) continue;
      fdMenuMapper.insertMenuAuth(cd.trim(), menuId, now);
    }
  }

  @Transactional
  public void insertSeed(FdMenu menu, List<String> authCds) {
    if (fdMenuMapper.selectById(menu.getMenuId()) != null) {
      return;
    }
    String now = LocalDateTime.now().format(FMT);
    menu.setCreateDt(now);
    if (menu.getUseYn() == null) menu.setUseYn("Y");
    if (menu.getIsLeaf() == null) menu.setIsLeaf("Y");
    if (menu.getRestrictedYn() == null) menu.setRestrictedYn("N");
    if (menu.getMenuLevel() == null) menu.setMenuLevel(1);
    if (menu.getSortOrder() == null) menu.setSortOrder(0);
    if (menu.getRealOrder() == null) menu.setRealOrder(menu.getSortOrder());
    fdMenuMapper.insert(menu);
    if (authCds != null) {
      for (String cd : authCds) {
        fdMenuMapper.insertMenuAuth(cd, menu.getMenuId(), now);
      }
    }
  }

  /** 시드/마이그레이션: 없으면 insert, 있으면 계층·명칭 갱신 */
  @Transactional
  public void upsertHierarchy(FdMenu menu, List<String> authCds) {
    FdMenu existing = fdMenuMapper.selectById(menu.getMenuId());
    if (existing == null) {
      insertSeed(menu, authCds);
      return;
    }
    existing.setMenuNm(menu.getMenuNm());
    existing.setMenuKey(menu.getMenuKey());
    existing.setPageUrl(menu.getPageUrl());
    existing.setApiPrefix(menu.getApiPrefix());
    existing.setUpMenuId(menu.getUpMenuId());
    existing.setMenuLevel(menu.getMenuLevel());
    existing.setIsLeaf(menu.getIsLeaf());
    existing.setSortOrder(menu.getSortOrder());
    existing.setRealOrder(menu.getRealOrder());
    existing.setIcon(menu.getIcon());
    existing.setRestrictedYn(menu.getRestrictedYn());
    existing.setUseYn(menu.getUseYn() == null ? "Y" : menu.getUseYn());
    existing.setUpdateDt(LocalDateTime.now().format(FMT));
    fdMenuMapper.update(existing);
    if (authCds != null) {
      replaceAuth(menu.getMenuId(), authCds);
    }
  }
}
