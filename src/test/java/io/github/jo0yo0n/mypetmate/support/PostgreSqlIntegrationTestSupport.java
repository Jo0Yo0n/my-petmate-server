package io.github.jo0yo0n.mypetmate.support;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.TestTransaction;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class PostgreSqlIntegrationTestSupport {

  @ServiceConnection
  static final PostgreSQLContainer<?> postgresql =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

  static {
    postgresql.start();
  }

  @Autowired private JdbcTemplate jdbcTemplate;

  // table 추가될 때마다 수정
  @AfterEach
  void clearDatabaseForNonTransactionalTests() {
    if (!TestTransaction.isActive()) {
      jdbcTemplate.execute("TRUNCATE TABLE guardian, refresh_token CASCADE");
    }
  }
}
