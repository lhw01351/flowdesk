package kr.flowdesk.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * schema.sql 의 CREATE IF NOT EXISTS 보장 (MySQL mode=never 대비)
 */
@Component
@Order(0)
public class SchemaBootstrap implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(SchemaBootstrap.class);

  private final DataSource dataSource;

  public SchemaBootstrap(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
      populator.addScript(new ClassPathResource("schema.sql"));
      populator.setContinueOnError(true);
      populator.execute(dataSource);
    } catch (Exception e) {
      log.warn("schema.sql 적용 경고: {}", e.getMessage());
    }
  }
}
