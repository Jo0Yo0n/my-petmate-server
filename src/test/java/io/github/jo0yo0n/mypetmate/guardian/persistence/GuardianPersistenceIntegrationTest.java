package io.github.jo0yo0n.mypetmate.guardian.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import io.github.jo0yo0n.mypetmate.guardian.domain.GuardianStatus;
import io.github.jo0yo0n.mypetmate.guardian.domain.IdentityVisibility;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import io.github.jo0yo0n.mypetmate.guardian.support.EmailNormalizer;
import io.github.jo0yo0n.mypetmate.support.PostgreSqlIntegrationTestSupport;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class GuardianPersistenceIntegrationTest extends PostgreSqlIntegrationTestSupport {

  @Autowired private Clock clock;
  @Autowired private EntityManager entityManager;
  @Autowired private GuardianRepository guardianRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private TransactionTemplate transactionTemplate;

  @DisplayName("[M1-JPA-01] savesGuardianAndRestoresNormalizedEmailAndEnums")
  @Test
  void savesGuardianAndRestoresNormalizedEmailAndEnums() {
    UUID guardianId = UUID.randomUUID();
    String email = EmailNormalizer.normalize("  Guardian@Example.com  ");
    Instant now = clock.instant();
    guardianRepository.saveAndFlush(
        new Guardian(
            guardianId,
            email,
            "password-hash",
            ProfileType.INDIVIDUAL,
            Gender.FEMALE,
            IdentityVisibility.PUBLIC,
            GuardianStatus.ACTIVE,
            now,
            now));
    entityManager.clear();

    Guardian guardian = guardianRepository.findByEmail(email).orElseThrow();

    assertThat(guardian.getId()).isEqualTo(guardianId);
    assertThat(guardian.getEmail()).isEqualTo(email);
    assertThat(guardianRepository.existsByEmail(email)).isTrue();
    assertThat(guardian.getProfileType()).isEqualTo(ProfileType.INDIVIDUAL);
    assertThat(guardian.getGender()).isEqualTo(Gender.FEMALE);
    assertThat(guardian.getIdentityVisibility()).isEqualTo(IdentityVisibility.PUBLIC);
    assertThat(guardian.getStatus()).isEqualTo(GuardianStatus.ACTIVE);
  }

  @DisplayName("[M1-JPA-04] rejectsDuplicateNormalizedEmail")
  @Test
  void rejectsDuplicateNormalizedEmail() {
    String localPart = "guardian-" + UUID.randomUUID();
    String email = EmailNormalizer.normalize(" " + localPart.toUpperCase() + "@Example.com ");
    String duplicateEmail = EmailNormalizer.normalize(localPart + "@example.com");
    guardianRepository.saveAndFlush(newGuardian(email));

    assertThat(duplicateEmail).isEqualTo(email);
    assertThatThrownBy(() -> guardianRepository.saveAndFlush(newGuardian(duplicateEmail)))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("uk_guardian_email_lower");
  }

  @DisplayName("[M1-JPA-02] savesRefreshTokenAndFindsItByHashWithGuardianAssociation")
  @Test
  void savesRefreshTokenAndFindsItByHashWithGuardianAssociation() {
    Guardian guardian = guardianRepository.saveAndFlush(newGuardian("token@example.com"));
    String tokenHash = "a".repeat(64);
    Instant now = clock.instant();
    refreshTokenRepository.saveAndFlush(
        new RefreshToken(UUID.randomUUID(), guardian, tokenHash, now.plusSeconds(60), null, now));
    entityManager.clear();

    RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash).orElseThrow();

    assertThat(refreshToken.getTokenHash()).isEqualTo(tokenHash);
    assertThat(refreshToken.getGuardian().getId()).isEqualTo(guardian.getId());
  }

  @DisplayName("[M1-JPA-03] pessimisticWriteLockSerializesConcurrentAccessToTheSameToken")
  @Test
  void pessimisticWriteLockSerializesConcurrentAccessToTheSameToken() throws Exception {
    Guardian guardian = guardianRepository.saveAndFlush(newGuardian("lock@example.com"));
    String tokenHash = "b".repeat(64);
    Instant now = clock.instant();
    refreshTokenRepository.saveAndFlush(
        new RefreshToken(UUID.randomUUID(), guardian, tokenHash, now.plusSeconds(60), null, now));

    CountDownLatch firstLockAcquired = new CountDownLatch(1);
    CountDownLatch allowFirstTransactionToCommit = new CountDownLatch(1);
    CountDownLatch secondLockAcquired = new CountDownLatch(1);

    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      var firstTransaction =
          executor.submit(
              () ->
                  transactionTemplate.executeWithoutResult(
                      status -> {
                        refreshTokenRepository.findByTokenHashWithPessimisticWriteLock(tokenHash);
                        firstLockAcquired.countDown();
                        await(allowFirstTransactionToCommit);
                      }));

      assertThat(firstLockAcquired.await(5, TimeUnit.SECONDS)).isTrue();

      var secondTransaction =
          executor.submit(
              () ->
                  transactionTemplate.executeWithoutResult(
                      status -> {
                        refreshTokenRepository.findByTokenHashWithPessimisticWriteLock(tokenHash);
                        secondLockAcquired.countDown();
                      }));

      assertThat(secondLockAcquired.await(250, TimeUnit.MILLISECONDS)).isFalse();
      allowFirstTransactionToCommit.countDown();
      firstTransaction.get(5, TimeUnit.SECONDS);
      secondTransaction.get(5, TimeUnit.SECONDS);
      assertThat(secondLockAcquired.getCount()).isZero();
    }
  }

  @Test
  void providesUtcClock() {
    assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
  }

  private Guardian newGuardian(String email) {
    Instant now = clock.instant();
    return new Guardian(
        UUID.randomUUID(),
        email,
        "password-hash",
        ProfileType.INDIVIDUAL,
        Gender.FEMALE,
        IdentityVisibility.PUBLIC,
        GuardianStatus.ACTIVE,
        now,
        now);
  }

  private void await(CountDownLatch latch) {
    try {
      if (!latch.await(5, TimeUnit.SECONDS)) {
        throw new AssertionError("Timed out waiting for the transaction to continue");
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError(
          "Interrupted while waiting for the transaction to continue", exception);
    }
  }
}
