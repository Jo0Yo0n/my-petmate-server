package io.github.jo0yo0n.mypetmate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class DatabaseMigrationIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> postgresql =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void appliesAllMigrationsToAnEmptyPostgresDatabase() {
    Boolean guardianAndRefreshTokenTableExists =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) = 2
            FROM information_schema.tables
            WHERE table_schema = 'public'
                AND table_name IN ('guardian', 'refresh_token')
            """,
            Boolean.class);
    Boolean emailIndexExists =
        jdbcTemplate.queryForObject(
            """
            SELECT EXISTS (
                SELECT 1
                FROM pg_indexes
                WHERE schemaname = 'public'
                    AND tablename = 'guardian'
                    AND indexname = 'uk_guardian_email_lower'
            )
            """,
            Boolean.class);

    assertThat(guardianAndRefreshTokenTableExists).isTrue();
    assertThat(emailIndexExists).isTrue();
  }

  @Test
  void guardianAcceptsValidEnumAndProfileTypeGenderCombinations() {
    assertThat(insertGuardian("individual@example.com", "individual", "female")).isNotNull();
    assertThat(insertGuardian("couple@example.com", "couple", null)).isNotNull();
    assertThat(insertGuardian("family@example.com", "family", null)).isNotNull();
  }

  @Test
  void guardianRejectsInvalidEnumAndProfileTypeGenderCombinations() {
    assertThatThrownBy(() -> insertGuardian("unknown-profile@example.com", "unknown", "female"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertGuardian("missing-gender@example.com", "individual", null))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertGuardian("couple-gender@example.com", "couple", "female"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                insertGuardian(
                    "unknown-visibility@example.com", "individual", "female", "hidden", "active"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                insertGuardian(
                    "unknown-status@example.com", "individual", "female", "public", "unknown"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void guardianRejectsEmailsThatDifferOnlyByCase() {
    insertGuardian("Guardian@Example.com", "individual", "female");

    assertThatThrownBy(() -> insertGuardian("guardian@example.com", "couple", null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void refreshTokenRejectsInvalidHashTimesAndGuardian() {
    UUID guardianId = insertGuardian("token-constraints@example.com", "individual", "female");
    Instant createdAt = Instant.parse("2026-08-26T00:00:00Z");

    assertThatThrownBy(
            () ->
                insertRefreshToken(
                    guardianId, "not-a-sha-256-hash", createdAt.plusSeconds(1), null, createdAt))
        .isInstanceOf(DataIntegrityViolationException.class);

    // expires_at > created_at
    assertThatThrownBy(
            () -> insertRefreshToken(guardianId, hashOf('a'), createdAt, null, createdAt))
        .isInstanceOf(DataIntegrityViolationException.class);

    // revoked_at >= created_at
    assertThatThrownBy(
            () ->
                insertRefreshToken(
                    guardianId,
                    hashOf('b'),
                    createdAt.plusSeconds(1),
                    createdAt.minusSeconds(1),
                    createdAt))
        .isInstanceOf(DataIntegrityViolationException.class);

    // guardian_id must exist
    assertThatThrownBy(
            () ->
                insertRefreshToken(
                    UUID.randomUUID(), hashOf('c'), createdAt.plusSeconds(1), null, createdAt))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void deletingGuardianDeletesRefreshTokens() {
    UUID guardianId = insertGuardian("cascade@example.com", "individual", "female");
    Instant createdAt = Instant.parse("2026-08-26T00:00:00Z");
    insertRefreshToken(guardianId, hashOf('d'), createdAt.plusSeconds(1), null, createdAt);

    jdbcTemplate.update("DELETE FROM guardian WHERE id = ?", guardianId);

    Integer refreshTokenCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM refresh_token WHERE guardian_id = ?", Integer.class, guardianId);
    assertThat(refreshTokenCount).isZero();
  }

  private UUID insertGuardian(String email, String profileType, String gender) {
    return insertGuardian(email, profileType, gender, "public", "active");
  }

  private UUID insertGuardian(
      String email, String profileType, String gender, String identityVisibility, String status) {
    UUID guardianId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO guardian (
            id, email, password_hash, profile_type, gender, identity_visibility, status)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
        guardianId,
        email,
        "password-hash",
        profileType,
        gender,
        identityVisibility,
        status);
    return guardianId;
  }

  private void insertRefreshToken(
      UUID guardianId, String tokenHash, Instant expiresAt, Instant revokedAt, Instant createdAt) {
    jdbcTemplate.update(
        """
        INSERT INTO refresh_token (id, guardian_id, token_hash, expires_at, revoked_at, created_at)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        UUID.randomUUID(),
        guardianId,
        tokenHash,
        Timestamp.from(expiresAt),
        revokedAt == null ? null : Timestamp.from(revokedAt),
        Timestamp.from(createdAt));
  }

  private String hashOf(char character) {
    return String.valueOf(character).repeat(64);
  }
}
