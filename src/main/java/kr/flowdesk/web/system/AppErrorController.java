package kr.flowdesk.web.system;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 없는 경로(404) → 미로그인: login.html / 로그인됨: home.html
 * API 요청은 JSON 404 유지
 */
@Controller
public class AppErrorController implements ErrorController {

  @RequestMapping("/error")
  public Object error(HttpServletRequest request, HttpServletResponse response) throws Exception {
    Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    int status = statusAttr == null ? 500 : Integer.parseInt(String.valueOf(statusAttr));

    String uri = String.valueOf(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
    String ctx = request.getContextPath() == null ? "" : request.getContextPath();
    String path = uri;
    if (ctx.length() > 0 && uri.startsWith(ctx)) {
      path = uri.substring(ctx.length());
    }
    if (path.isEmpty()) {
      path = "/";
    }

    boolean api = path.startsWith("/api/") || wantsJson(request);
    if (api || status != HttpStatus.NOT_FOUND.value()) {
      if (api) {
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"ok\":false,\"message\":\"요청한 리소스를 찾을 수 없습니다.\"}");
      }
      response.sendError(status);
      return null;
    }

    HttpSession session = request.getSession(false);
    boolean loggedIn = session != null && session.getAttribute(AuthController.SESSION_USER) != null;
    response.sendRedirect(ctx + (loggedIn ? "/home.html" : "/login.html"));
    return null;
  }

  private boolean wantsJson(HttpServletRequest request) {
    String accept = request.getHeader("Accept");
    return accept != null && accept.contains("application/json") && !accept.contains("text/html");
  }
}
