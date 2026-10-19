package io.github.jo0yo0n.mypetmate.guardian;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureMockMvc
class GuardianWithdrawalIntegrationTest extends GuardianWithdrawalTestSupport {
  @Test
  @DisplayName("[M1-API-WD-03] rollsBackWithdrawalWhenRefreshTokenRevocationFails")
  void rollsBackWithdrawalWhenRefreshTokenRevocationFails() throws Exception {
    var auth = signup();
    var id = id(auth);
    // Two rows ensure the assertion also protects against partial token revocation.
    jdbc.update(
        """
        INSERT INTO refresh_token (id, guardian_id, token_hash, expires_at, created_at)
        SELECT ?, guardian_id, ?, expires_at, created_at FROM refresh_token WHERE guardian_id = ?
        """,
        java.util.UUID.randomUUID(),
        "a".repeat(64),
        id);
    var guardianBefore = guardian(id);
    var tokensBefore = tokens(id);
    jdbc.execute(
        """
        CREATE FUNCTION fail_wd_token_revocation() RETURNS trigger LANGUAGE plpgsql AS $$
        BEGIN
          IF NEW.revoked_at IS NOT NULL AND OLD.revoked_at IS NULL THEN
            RAISE EXCEPTION 'WD_TEST_REVOCATION_FAILURE';
          END IF;
          RETURN NEW;
        END $$
        """);
    try {
      jdbc.execute(
          """
          CREATE TRIGGER fail_wd_token_revocation BEFORE UPDATE ON refresh_token
          FOR EACH ROW EXECUTE FUNCTION fail_wd_token_revocation()
          """);
      var result = withdraw(auth);
      assertProblem(result, 500, "INTERNAL_SERVER_ERROR");
      // A generic stub failure must not make this rollback test pass.
      assertThat(result.getResolvedException()).isNotNull();
      Throwable cause = result.getResolvedException();
      while (cause.getCause() != null) cause = cause.getCause();
      assertThat(cause.getMessage()).contains("WD_TEST_REVOCATION_FAILURE");
      assertThat(guardian(id)).isEqualTo(guardianBefore);
      assertThat(tokens(id)).isEqualTo(tokensBefore);
      assertThat(result.getResponse().getContentAsString())
          .doesNotContain("WD_TEST_REVOCATION_FAILURE");
    } finally {
      jdbc.execute("DROP TRIGGER IF EXISTS fail_wd_token_revocation ON refresh_token");
      jdbc.execute("DROP FUNCTION fail_wd_token_revocation()");
    }
  }
}
