package kr.flowdesk.config;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import kr.flowdesk.domain.AppUser;
import kr.flowdesk.domain.FdAuth;
import kr.flowdesk.domain.FdMenu;
import kr.flowdesk.service.AppUserService;
import kr.flowdesk.service.FdAuthService;
import kr.flowdesk.service.FdMenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 권한·메뉴 시드 + 계정 마이그레이션(이름/권한/하니)
 */
@Component
@Order(2)
public class AuthDataBootstrap implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AuthDataBootstrap.class);
  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final DataSource dataSource;
  private final FdAuthService fdAuthService;
  private final FdMenuService fdMenuService;
  private final AppUserService appUserService;

  public AuthDataBootstrap(
      DataSource dataSource,
      FdAuthService fdAuthService,
      FdMenuService fdMenuService,
      AppUserService appUserService) {
    this.dataSource = dataSource;
    this.fdAuthService = fdAuthService;
    this.fdMenuService = fdMenuService;
    this.appUserService = appUserService;
  }

  @Override
  public void run(ApplicationArguments args) {
    ensureAuthCdColumn();
    ensureProfileImageColumn();
    seedAuth();
    seedMenus();
    migrateAndSeedUsers();
  }

  private void ensureAuthCdColumn() {
    try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
      st.execute("ALTER TABLE app_users ADD COLUMN auth_cd VARCHAR(10)");
      log.info("app_users.auth_cd 컬럼 추가");
    } catch (Exception ignored) {
      // 이미 있으면 무시
    }
  }

  private void ensureProfileImageColumn() {
    try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
      st.execute("ALTER TABLE app_users ADD COLUMN profile_image VARCHAR(200)");
      log.info("app_users.profile_image 컬럼 추가");
    } catch (Exception ignored) {
      // 이미 있으면 무시
    }
  }

  private void seedAuth() {
    if (fdAuthService.count() > 0) return;
    log.info("fd_auth 시드");
    String now = LocalDateTime.now().format(FMT);
    FdAuth a1 = new FdAuth();
    a1.setAuthCd(FdAuthService.AUTH_ADMIN);
    a1.setAuthNm("전체관리자");
    a1.setSortOrder(1);
    a1.setUseYn("Y");
    a1.setCreateDt(now);
    a1.setNote("전체 관리 권한");
    fdAuthService.create(a1);

    FdAuth a2 = new FdAuth();
    a2.setAuthCd(FdAuthService.AUTH_USER);
    a2.setAuthNm("일반사용자");
    a2.setSortOrder(2);
    a2.setUseYn("Y");
    a2.setCreateDt(now);
    a2.setNote("일반 사용자");
    fdAuthService.create(a2);
  }

  private void seedMenus() {
    log.info("fd_menu 계층 동기화 (대 > 중 > 소)");
    List<String> all = Arrays.asList(FdAuthService.AUTH_ADMIN, FdAuthService.AUTH_USER);
    List<String> adminOnly = Arrays.asList(FdAuthService.AUTH_ADMIN);

    // 대메뉴 (폴더 / 단독)
    upsertMenu("HOME", "home", "홈", null, 1, "Y",
        "/home.html", "/api/inbody", "홈", 10, "N", all);
    upsertMenu("G_WORK", "work", "업무 페이지", null, 1, "N",
        null, null, "업무", 20, "N", null);
    upsertMenu("G_PERSONAL", "personal", "개인 페이지", null, 1, "N",
        null, null, "개인", 30, "N", null);
    upsertMenu("G_ADMIN", "admin", "관리 페이지", null, 1, "N",
        null, null, "관리", 40, "Y", null);

    // 중메뉴 — 문의 트래커: 일반사용자(guest 포폴 포함)도 본인 데이터만 조회
    upsertMenu("CX", "cx", "문의 트래커", "G_WORK", 2, "Y",
        "/index.html", "/api/inquiries", "문의", 21, "N", all);
    upsertMenu("HEALTH", "health", "인바디 케어", "G_PERSONAL", 2, "Y",
        "/health.html", "/api/inbody", "IB", 31, "N", all);
    upsertMenu("REMIND", "remind", "리마인드", "G_PERSONAL", 2, "Y",
        "/remind.html", "/api/reminders", "RM", 32, "N", all);
    upsertMenu("USERS", "users", "회원 관리", "G_ADMIN", 2, "Y",
        "/users.html", "/api/users", "USR", 41, "Y", adminOnly);
    upsertMenu("MENUS", "menus", "메뉴 관리", "G_ADMIN", 2, "Y",
        "/menus.html", "/api/menus", "MNU", 42, "Y", adminOnly);
    upsertMenu("ACCESS", "access", "로그인 로그", "G_ADMIN", 2, "Y",
        "/access.html", "/api/access-logs", "LOG", 43, "Y", adminOnly);
    upsertMenu("CHATFILES", "chatfiles", "채팅 첨부", "G_ADMIN", 2, "Y",
        "/chat-files.html", "/api/chat/admin", "CH", 44, "Y", adminOnly);
  }

  private void upsertMenu(
      String id, String key, String nm, String upId, int level, String leaf,
      String page, String api, String icon, int order, String restricted, List<String> authCds) {
    FdMenu m = new FdMenu();
    m.setMenuId(id);
    m.setMenuKey(key);
    m.setMenuNm(nm);
    m.setUpMenuId(upId);
    m.setMenuLevel(level);
    m.setIsLeaf(leaf);
    m.setPageUrl(page);
    m.setApiPrefix(api);
    m.setIcon(icon);
    m.setSortOrder(order);
    m.setRealOrder(order);
    m.setRestrictedYn(restricted);
    m.setUseYn("Y");
    m.setSourceType("A");
    m.setPath(page);
    fdMenuService.upsertHierarchy(m, authCds);
  }

  private void migrateAndSeedUsers() {
    renameUser("cxadmin", "admin", "관리자", FdAuthService.AUTH_ADMIN);
    renameUser("guest_user", "guest", "손님", FdAuthService.AUTH_USER);
    renameUser("guest_folio", "guest", "손님", FdAuthService.AUTH_USER);

    ensureUser("admin", "cxadmin123", "관리자", FdAuthService.AUTH_ADMIN);
    ensureUser("hwlee", "hwlee1234", "이현욱", FdAuthService.AUTH_ADMIN);
    ensureUser("guest", "guest", "손님", FdAuthService.AUTH_USER);
    ensureUser("hani", "hani1234", "김하니", FdAuthService.AUTH_ADMIN);

    // 표시명·권한 보정
    patchUser("admin", "관리자", FdAuthService.AUTH_ADMIN);
    patchUser("hwlee", "이현욱", FdAuthService.AUTH_ADMIN);
    patchUser("guest", "손님", FdAuthService.AUTH_USER);
    patchUser("hani", "김하니", FdAuthService.AUTH_ADMIN);

    // 포폴용 게스트: guest / guest
    ensurePassword("guest", "guest");
  }

  private void renameUser(String from, String to, String display, String authCd) {
    AppUser old = appUserService.findByUsername(from);
    if (old == null) return;
    if (appUserService.findByUsername(to) != null) {
      // 대상이 이미 있으면 구계정만 정리
      if (!from.equals(to)) {
        try {
          appUserService.delete(old.getId());
          log.info("중복 구계정 삭제: {}", from);
        } catch (Exception e) {
          log.warn("구계정 삭제 실패 {}: {}", from, e.getMessage());
        }
      }
      return;
    }
    old.setUsername(to);
    old.setDisplayName(display);
    old.setAuthCd(authCd);
    old.setRole(AppUserService.roleFromAuthCd(authCd));
    appUserService.update(old.getId(), old);
    log.info("계정명 변경 {} → {}", from, to);
  }

  private void ensureUser(String username, String password, String display, String authCd) {
    if (appUserService.findByUsername(username) != null) return;
    AppUser u = new AppUser();
    u.setUsername(username);
    u.setPassword(password);
    u.setDisplayName(display);
    u.setAuthCd(authCd);
    appUserService.create(u);
    log.info("계정 시드: {} ({})", username, display);
  }

  private void patchUser(String username, String display, String authCd) {
    AppUser u = appUserService.findByUsername(username);
    if (u == null) return;
    boolean dirty = false;
    AppUser patch = new AppUser();
    patch.setUsername(u.getUsername());
    patch.setDisplayName(display);
    patch.setAuthCd(authCd);
    patch.setNote(u.getNote());
    if (display != null && !display.equals(u.getDisplayName())) dirty = true;
    if (authCd != null && !authCd.equals(u.getAuthCd())) dirty = true;
    if (dirty) {
      appUserService.update(u.getId(), patch);
    }
  }

  /** 기존 계정의 데모 비밀번호를 목표 값으로 맞춤 (포폴 게스트 등) */
  private void ensurePassword(String username, String password) {
    AppUser u = appUserService.findByUsername(username);
    if (u == null || password == null || password.isEmpty()) return;
    if (password.equals(u.getPassword())) return;
    AppUser patch = new AppUser();
    patch.setUsername(u.getUsername());
    patch.setPassword(password);
    patch.setDisplayName(u.getDisplayName());
    patch.setAuthCd(u.getAuthCd());
    patch.setNote(u.getNote());
    appUserService.update(u.getId(), patch);
    log.info("계정 비밀번호 동기화: {}", username);
  }
}
