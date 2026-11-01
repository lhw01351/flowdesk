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

/** MySQL mode=never 대비 채팅 테이블 보장 */
@Component
@Order(1)
public class ChatBootstrap implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(ChatBootstrap.class);

  private final DataSource dataSource;

  public ChatBootstrap(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void run(ApplicationArguments args) {
    try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
      st.execute(
          "CREATE TABLE IF NOT EXISTS chat_messages ("
              + "id VARCHAR(64) NOT NULL PRIMARY KEY,"
              + "username VARCHAR(100),"
              + "display_name VARCHAR(100),"
              + "body_text VARCHAR(4000),"
              + "created_at VARCHAR(32) NOT NULL"
              + ")");
      st.execute(
          "CREATE TABLE IF NOT EXISTS chat_attachments ("
              + "id VARCHAR(64) NOT NULL PRIMARY KEY,"
              + "message_id VARCHAR(64),"
              + "original_name VARCHAR(300),"
              + "stored_name VARCHAR(300) NOT NULL,"
              + "content_type VARCHAR(120),"
              + "size_bytes BIGINT,"
              + "uploaded_by VARCHAR(100),"
              + "uploaded_at VARCHAR(32) NOT NULL"
              + ")");
      log.info("chat 테이블 확인 완료");
    } catch (Exception e) {
      log.warn("chat 테이블 보장 실패: {}", e.getMessage());
    }
  }
}
