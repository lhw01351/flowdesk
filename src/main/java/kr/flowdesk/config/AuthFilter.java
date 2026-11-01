package kr.flowdesk.config;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import kr.flowdesk.service.MenuAccessService;
import kr.flowdesk.web.AuthController;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 미로그인 차단 + 권한(메뉴) 기반 접근 제어
 * 회원·메뉴·권한 API: admin·hwlee (MenuAccessService.isManager)
 */
@Component
@Order(1)
public class AuthFilter extends OncePerRequestFilter {

  private final MenuAccessService menuAccessService;

  public AuthFilter(MenuAccessService menuAccessService) {
    this.menuAccessService = menuAccessService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String ctx = request.getContextPath() == null ? "" : request.getContextPath();
    String uri = request.getRequestURI();
    String path = uri.substring(ctx.length());
    if (path.isEmpty()) {
      path = "/";
    }

    if (isPublic(path)) {
      filterChain.doFilter(request, response);
      return;
    }

    HttpSession session = request.getSession(false);
    boolean ok = session != null && session.getAttribute(AuthController.SESSION_USER) != null;

    if (!ok) {
      if (path.startsWith("/api/")) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"ok\":false,\"message\":\"로그인이 필요합니다.\"}");
        return;
      }
      response.sendRedirect(ctx + "/login.html");
      return;
    }

    String username = AuthController.usernameOf(session);
    String authCd = AuthController.authCdOf(session);

    // 회원·메뉴·권한 API/화면: admin·hwlee
    if (isManagerApi(path)) {
      if (!MenuAccessService.isManager(session)) {
        deny(path, response, ctx);
        return;
      }
      filterChain.doFilter(request, response);
      return;
    }

    // 접속 로그 기록(POST)은 로그인 사용자 모두 허용 (조회 GET은 메뉴 권한)
    if ("/api/access-logs".equals(path) && "POST".equalsIgnoreCase(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }

    // 홈 채팅 API·WebSocket: 로그인 사용자 모두 허용 (관리자 첨부 API는 컨트롤러에서 재검증)
    if (path.startsWith("/api/chat") || path.startsWith("/ws-chat")) {
      filterChain.doFilter(request, response);
      return;
    }

    // 그 외 보호 경로: 메뉴 권한
    if (needsMenuCheck(path) && !menuAccessService.canAccessPath(username, authCd, path)) {
      deny(path, response, ctx);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private boolean needsMenuCheck(String path) {
    if ("/home.html".equals(path) || "/".equals(path)) return false;
    if (path.startsWith("/api/auth/")) return false;
    if (path.endsWith(".html")) return true;
    if (path.startsWith("/api/")) return true;
    return false;
  }

  private boolean isManagerApi(String path) {
    if ("/users.html".equals(path) || "/menus.html".equals(path)) return true;
    if (path.startsWith("/api/users") || path.startsWith("/api/menus") || path.startsWith("/api/auths")) {
      return true;
    }
    return false;
  }

  private void deny(String path, HttpServletResponse response, String ctx) throws IOException {
    if (path.startsWith("/api/")) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"ok\":false,\"message\":\"권한이 없습니다.\"}");
      return;
    }
    response.sendRedirect(ctx + "/home.html");
  }

  private boolean isPublic(String path) {
    if ("/login.html".equals(path) || "/error".equals(path)) {
      return true;
    }
    if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/me")) {
      return true;
    }
    if (path.startsWith("/css/") || path.startsWith("/js/")) {
      return true;
    }
    if (path.endsWith(".ico") || path.endsWith(".png") || path.endsWith(".svg")
        || path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".woff")
        || path.endsWith(".woff2") || path.endsWith(".map")) {
      return true;
    }
    return false;
  }
}
