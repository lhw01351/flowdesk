package kr.flowdesk;

import kr.flowdesk.config.AuthProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * FlowDesk — 개인 워크스페이스 진입점
 * - java -jar : main()
 * - 외부 Tomcat WAR : SpringBootServletInitializer
 */
@SpringBootApplication
@MapperScan("kr.flowdesk.mapper")
@EnableConfigurationProperties(AuthProperties.class)
public class FlowDeskApplication extends SpringBootServletInitializer {

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
    return builder.sources(FlowDeskApplication.class);
  }

  public static void main(String[] args) {
    SpringApplication.run(FlowDeskApplication.class, args);
  }
}
