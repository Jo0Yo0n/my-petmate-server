package io.github.jo0yo0n.mypetmate.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import io.github.jo0yo0n.mypetmate.auth.exception.EmailAlreadyExistsException;
import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import io.github.jo0yo0n.mypetmate.guardian.domain.IdentityVisibility;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import io.github.jo0yo0n.mypetmate.guardian.dto.AuthResponse;
import io.github.jo0yo0n.mypetmate.guardian.dto.SignupRequest;
import io.github.jo0yo0n.mypetmate.guardian.persistence.GuardianRepository;
import io.github.jo0yo0n.mypetmate.guardian.persistence.RefreshToken;
import io.github.jo0yo0n.mypetmate.guardian.persistence.RefreshTokenRepository;
import io.github.jo0yo0n.mypetmate.support.PostgreSqlIntegrationTestSupport;
import java.util.List;
import java.util.UUID;
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
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
public class AuthServiceSignupConcurrencyIntegrationTest extends PostgreSqlIntegrationTestSupport {

  @Autowired private AuthService authService;
  @MockitoSpyBean private GuardianRepository guardianRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private RefreshTokenGenerator refreshTokenGenerator;

  @DisplayName("[M1-AUTH-12] allowOnlyOneConcurrentSignupForSameEmail")
  @Test
  void allowOnlyOneConcurrentSignupForSameEmail() throws Exception {

    // given
    String canonicalEmail = "guardian-" + UUID.randomUUID() + "@example.com";
    SignupRequest signupRequest =
        new SignupRequest(
            canonicalEmail,
            "StringPassword1!",
            ProfileType.INDIVIDUAL,
            Gender.MALE,
            IdentityVisibility.PUBLIC);

    CountDownLatch bothPassedDuplicateCheck = new CountDownLatch(2);
    CountDownLatch allowSignupToContinue = new CountDownLatch(1);

    doAnswer(
            invocation -> {
              bothPassedDuplicateCheck.countDown();
              await(allowSignupToContinue);
              return false;
            })
        .when(guardianRepository)
        .existsByEmail(anyString());

    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      long beforeGuardianCount = guardianRepository.count();
      long beforeRefreshTokenCount = refreshTokenRepository.count();

      // when
      var firstTransaction = executor.submit(() -> authService.signup(signupRequest));
      var secondTransaction = executor.submit(() -> authService.signup(signupRequest));

      await(bothPassedDuplicateCheck);
      allowSignupToContinue.countDown();

      // then
      Result firstResult = getResult(firstTransaction);
      Result secondResult = getResult(secondTransaction);

      Throwable failure = firstResult.failure != null ? firstResult.failure : secondResult.failure;
      AuthResponse authResponse =
          firstResult.authResponse != null ? firstResult.authResponse : secondResult.authResponse;

      assertThat(guardianRepository.count()).isEqualTo(beforeGuardianCount + 1);
      assertThat(refreshTokenRepository.count()).isEqualTo(beforeRefreshTokenCount + 1);

      List<Result> results = List.of(firstResult, secondResult);
      assertThat(results).filteredOn(result -> result.authResponse() != null).hasSize(1);
      assertThat(results)
          .filteredOn(result -> result.failure instanceof EmailAlreadyExistsException)
          .hasSize(1);

      transactionTemplate.executeWithoutResult(
          status -> {
            RefreshToken refreshToken =
                refreshTokenRepository
                    .findByTokenHash(refreshTokenGenerator.hash(authResponse.refreshToken()))
                    .orElseThrow();

            assertThat(refreshToken.getGuardian().getEmail()).isEqualTo(canonicalEmail);
          });
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

  private Result getResult(Future<AuthResponse> future)
      throws InterruptedException, TimeoutException {
    try {
      return new Result(future.get(5, TimeUnit.SECONDS), null);

    } catch (ExecutionException e) {
      return new Result(null, e.getCause());
    }
  }

  private record Result(AuthResponse authResponse, Throwable failure) {}
}
