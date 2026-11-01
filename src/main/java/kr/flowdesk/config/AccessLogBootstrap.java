package kr.flowdesk.config;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 접속 로그 컬럼 마이그레이션 (menu_nm) */
@Component
@Order(5)
public class AccessLogBootstrap implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AccessLogBootstrap.class);

  private final DataSource dataSource;

  public AccessLogBootstrap(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void run(ApplicationArguments args) {
    alter("ALTER TABLE access_logs ADD COLUMN menu_nm VARCHAR(100)");
  }

  private void alter(String sql) {
    try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
      st.execute(sql);
      log.info("스키마: {}", sql);
    } catch (Exception ignored) {
    }
  }
}
