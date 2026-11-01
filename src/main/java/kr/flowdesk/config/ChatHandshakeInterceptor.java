package kr.flowdesk.config;

import java.util.Map;
import javax.servlet.http.HttpSession;
import kr.flowdesk.web.AuthController;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/** WebSocket 핸드셰이크 시 로그인 세션을 소켓 속성에 복사 */
@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

  public static final String ATTR_USER = "CHAT_USER";
  public static final String ATTR_DISPLAY = "CHAT_DISPLAY";

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {
    if (!(request instanceof ServletServerHttpRequest)) {
      return false;
    }
    HttpSession session = ((ServletServerHttpRequest) request).getServletRequest().getSession(false);
    if (session == null) return false;
    Object user = session.getAttribute(AuthController.SESSION_USER);
    if (user == null) return false;
    attributes.put(ATTR_USER, String.valueOf(user));
    Object display = session.getAttribute(AuthController.SESSION_DISPLAY);
    if (display == null) display = user;
    attributes.put(ATTR_DISPLAY, String.valueOf(display));
    return true;
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {
    // no-op
  }
}
