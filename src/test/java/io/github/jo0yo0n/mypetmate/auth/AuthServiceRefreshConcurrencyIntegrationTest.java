package io.github.jo0yo0n.mypetmate.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;

import io.github.jo0yo0n.mypetmate.auth.dto.AuthResponse;
import io.github.jo0yo0n.mypetmate.auth.dto.RefreshRequest;
import io.github.jo0yo0n.mypetmate.auth.dto.SignupRequest;
import io.github.jo0yo0n.mypetmate.auth.dto.TokenResponse;
import io.github.jo0yo0n.mypetmate.auth.exception.InvalidRefreshTokenException;
import io.github.jo0yo0n.mypetmate.guardian.domain.IdentityVisibility;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import io.github.jo0yo0n.mypetmate.guardian.persistence.RefreshToken;
import io.github.jo0yo0n.mypetmate.guardian.persistence.RefreshTokenRepository;
import io.github.jo0yo0n.mypetmate.support.PostgreSqlIntegrationTestSupport;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
public class AuthServiceRefreshConcurrencyIntegrationTest extends PostgreSqlIntegrationTestSupport {

  @Autowired private AuthService authService;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private RefreshTokenGenerator refreshTokenGenerator;
  @MockitoSpyBean private AccessTokenIssuer accessTokenIssuer;

  @DisplayName("[M1-AUTH-09] allowOnlyOneConcurrencyRefreshRequest")
  @Test
  void allowOnlyOneConcurrencyRefreshRequest() throws Exception {

    // given
    SignupRequest signupRequest =
        new SignupRequest(
            "guardian@example.com",
            "StrongPassword1!",
            ProfileType.FAMILY,
            null,
            IdentityVisibility.PUBLIC);

    AuthResponse authResponse = authService.signup(signupRequest);
    String rawRefreshToken = authResponse.refreshToken();
    RefreshRequest refreshRequest = new RefreshRequest(rawRefreshToken);
    long beforeRefreshCount = refreshTokenRepository.count();

    CountDownLatch bothReadyToRefresh = new CountDownLatch(2);
    CountDownLatch releaseRefresh = new CountDownLatch(1);

    clearInvocations(accessTokenIssuer);

    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {

      // when
      var firstTransaction =
          executor.submit(
              () -> {
                bothReadyToRefresh.countDown();
                await(releaseRefresh);

                return authService.refresh(refreshRequest);
              });
      var secondTransaction =
          executor.submit(
              () -> {
                bothReadyToRefresh.countDown();
                await(releaseRefresh);

                return authService.refresh(refreshRequest);
              });

      await(bothReadyToRefresh);
      releaseRefresh.countDown();

      // then
      Result firstResult = getResult(firstTransaction);
      Result secondResult = getResult(secondTransaction);

      List<Result> results = List.of(firstResult, secondResult);
      TokenResponse tokenResponse =
          results.stream()
              .filter(result -> result.tokenResponse != null)
              .map(result -> result.tokenResponse)
              .findFirst()
              .orElseThrow();

      assertThat(results).filteredOn(result -> result.tokenResponse != null).hasSize(1);
      assertThat(results)
          .filteredOn(result -> result.failure instanceof InvalidRefreshTokenException)
          .hasSize(1);
      then(accessTokenIssuer).should().issue(any(), any());

      RefreshToken legacyRefreshToken =
          refreshTokenRepository
              .findByTokenHash(refreshTokenGenerator.hash(authResponse.refreshToken()))
              .orElseThrow();
      assertThat(legacyRefreshToken.getRevokedAt()).isNotNull();
      assertThat(refreshTokenRepository.count()).isEqualTo(beforeRefreshCount + 1);
      assertThat(
              refreshTokenRepository.findByTokenHash(
                  refreshTokenGenerator.hash(tokenResponse.refreshToken())))
          .hasValueSatisfying(refreshToken -> assertThat(refreshToken.getRevokedAt()).isNull());
    }
  }

  record Result(TokenResponse tokenResponse, Throwable failure) {}

  private Result getResult(Future<TokenResponse> future)
      throws InterruptedException, TimeoutException {
    try {
      return new Result(future.get(5, TimeUnit.SECONDS), null);
    } catch (ExecutionException e) {
      return new Result(null, e.getCause());
    }
  }

  private void await(CountDownLatch latch) {
    try {
      if (!latch.await(5, TimeUnit.SECONDS)) {
        throw new AssertionError("Timed out waiting for the transaction to continue");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while waiting for the transaction to continue", e);
    }
  }
}
