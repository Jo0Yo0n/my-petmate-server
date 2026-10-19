package io.github.jo0yo0n.mypetmate.guardian;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;

@SpringBootTest
@AutoConfigureMockMvc
class GuardianWithdrawalConcurrencyIntegrationTest extends GuardianWithdrawalTestSupport {
  @Test
  @DisplayName("[M1-API-WD-04] rejectsTokensAfterConcurrentRefreshAndWithdrawal")
  void rejectsTokensAfterConcurrentRefreshAndWithdrawal() throws Exception {
    var auth = signup();
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      try {
        var refresh =
            executor.submit(
                () -> {
                  awaitStart(ready, start);
                  return refresh(auth);
                });
        var withdrawal =
            executor.submit(
                () -> {
                  awaitStart(ready, start);
                  return withdraw(auth);
                });
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        var withdrawn = withdrawal.get(15, TimeUnit.SECONDS);
        var refreshed = refresh.get(15, TimeUnit.SECONDS);
        assertThat(withdrawn.getResponse().getStatus()).isEqualTo(204);
        assertThat(withdrawn.getResponse().getContentAsString()).isEmpty();
        assertThat(guardian(id(auth)).get("status")).isEqualTo("withdrawn");
        assertThat(tokens(id(auth)))
            .isNotEmpty()
            .allSatisfy(token -> assertThat(token.get("revoked_at")).isNotNull());

        assertTokensRejected(auth);
        if (refreshed.getResponse().getStatus() == 200) {
          assertTokensRejected(mapper.readTree(refreshed.getResponse().getContentAsString()));
        } else {
          assertProblem(refreshed, 401, "AUTH_REFRESH_INVALID");
        }
      } finally {
        start.countDown();
      }
    }
  }

  private void assertTokensRejected(JsonNode tokens) throws Exception {
    var access =
        mvc.perform(
                get("/api/guardians/me")
                    .header(
                        HttpHeaders.AUTHORIZATION, "Bearer " + tokens.path("accessToken").asText()))
            .andReturn();
    assertProblem(access, 403, "ACCOUNT_WITHDRAWN");
    assertProblem(refresh(tokens), 401, "AUTH_REFRESH_INVALID");
  }

  private void awaitStart(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
    ready.countDown();
    if (!start.await(5, TimeUnit.SECONDS))
      throw new AssertionError("Timed out starting withdrawal race");
  }
}
