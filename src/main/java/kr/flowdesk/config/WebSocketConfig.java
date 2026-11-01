package kr.flowdesk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final ChatHandshakeInterceptor chatHandshakeInterceptor;
  private final ChatAuthChannelInterceptor chatAuthChannelInterceptor;

  public WebSocketConfig(
      ChatHandshakeInterceptor chatHandshakeInterceptor,
      ChatAuthChannelInterceptor chatAuthChannelInterceptor) {
    this.chatHandshakeInterceptor = chatHandshakeInterceptor;
    this.chatAuthChannelInterceptor = chatAuthChannelInterceptor;
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws-chat")
        .addInterceptors(chatHandshakeInterceptor)
        .setAllowedOriginPatterns("*")
        .withSockJS();
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic");
    registry.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(chatAuthChannelInterceptor);
  }
}
