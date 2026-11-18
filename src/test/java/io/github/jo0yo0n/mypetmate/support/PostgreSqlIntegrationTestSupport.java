package io.github.jo0yo0n.mypetmate.support;

import java.util.UUID;
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

  public static final String ANSAN_REGIONS_SQL = "file:scripts/fixtures/ansan-regions.sql";
  public static final UUID IL_DONG_ID = UUID.fromString("c6cd82c5-907c-50f8-b341-44c379916e78");

  @ServiceConnection
  static final PostgreSQLContainer<?> postgresql =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

  static {
    postgresql.start();
  }

  @Autowired private JdbcTemplate jdbcTemplate;

  // table 추가될 때마다 수정
  @AfterEach
  protected void clearDatabaseForNonTransactionalTests() {
    if (!TestTransaction.isActive()) {
      jdbcTemplate.execute("TRUNCATE TABLE guardian, refresh_token, region CASCADE");
    }
  }
}
