package kr.flowdesk.config;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 앱 컨텍스트 루트(/flowdesk/) → home.html (미로그인은 AuthFilter가 login으로)
 * API CORS · 세션 타임아웃
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  /** Servlet 4: 분 단위. YAML 2h / web.xml 120 과 동일하게 맞춤 */
  private static final int SESSION_TIMEOUT_MINUTES = 120;

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/").setViewName("forward:/home.html");
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**").allowedOriginPatterns("*").allowedMethods("*");
  }

  @Bean
  public ServletContextInitializer sessionTimeoutInitializer() {
    return servletContext -> servletContext.setSessionTimeout(SESSION_TIMEOUT_MINUTES);
  }
}
