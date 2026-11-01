package kr.flowdesk.config;

import java.util.Map;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/** STOMP CONNECT/SEND 시 핸드셰이크에 심은 로그인 사용자 확인 */
@Component
public class ChatAuthChannelInterceptor implements ChannelInterceptor {

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null) return message;
    StompCommand cmd = accessor.getCommand();
    if (cmd != StompCommand.CONNECT && cmd != StompCommand.SEND) {
      return message;
    }
    Map<String, Object> attrs = accessor.getSessionAttributes();
    Object user = attrs == null ? null : attrs.get(ChatHandshakeInterceptor.ATTR_USER);
    if (user == null || String.valueOf(user).trim().isEmpty()) {
      throw new IllegalArgumentException("로그인이 필요합니다.");
    }
    final String name = String.valueOf(user);
    accessor.setUser(() -> name);
    return message;
  }
}
