package kr.flowdesk.config;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import kr.flowdesk.service.content.InquiryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 문의 트래커 소유자 컬럼 마이그레이션.
 * 기존 데이터(hwlee 업무분) → create_user = hwlee
 */
@Component
@Order(4)
public class InquiryBootstrap implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(InquiryBootstrap.class);
  private static final String DEFAULT_OWNER = "hwlee";

  private final DataSource dataSource;
  private final InquiryService inquiryService;

  public InquiryBootstrap(DataSource dataSource, InquiryService inquiryService) {
    this.dataSource = dataSource;
    this.inquiryService = inquiryService;
  }

  @Override
  public void run(ApplicationArguments args) {
    alter("ALTER TABLE inquiries ADD COLUMN create_user VARCHAR(100)");
    int filled = inquiryService.backfillOwner(DEFAULT_OWNER);
    if (filled > 0) {
      log.info("문의 create_user 보정 {}건 → {}", filled, DEFAULT_OWNER);
    }
  }

  private void alter(String sql) {
    try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
      st.execute(sql);
      log.info("스키마: {}", sql);
    } catch (Exception ignored) {
      // 이미 있으면 무시
    }
  }
}
