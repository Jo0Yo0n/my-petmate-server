package io.github.jo0yo0n.mypetmate.guardian.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.jo0yo0n.mypetmate.guardian.domain.*;
import io.github.jo0yo0n.mypetmate.guardian.support.EmailNormalizer;
import io.github.jo0yo0n.mypetmate.support.PostgreSqlIntegrationTestSupport;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class GuardianPersistenceIntegrationTest extends PostgreSqlIntegrationTestSupport {

  @Autowired private Clock clock;
  @Autowired private EntityManager entityManager;
  @Autowired private GuardianRepository guardianRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

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
            DiscoveryStatus.SEARCHING,
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
    assertThat(guardian.getDiscoveryStatus()).isEqualTo(DiscoveryStatus.SEARCHING);
    assertThat(guardian.getStatus()).isEqualTo(GuardianStatus.ACTIVE);
  }

  @DisplayName("[보강] rejectsDuplicateNormalizedEmail")
  @Test
  void rejectsDuplicateNormalizedEmail() {
    String localPart = "guardian-" + UUID.randomUUID();
    String email = EmailNormalizer.normalize(" " + localPart.toUpperCase() + "@Example.com ");
    String duplicateEmail = EmailNormalizer.normalize(localPart + "@example.com");
    guardianRepository.saveAndFlush(newGuardian(email));

    assertThat(duplicateEmail).isEqualTo(email);
    assertThatThrownBy(() -> guardianRepository.saveAndFlush(newGuardian(duplicateEmail)))
        .isInstanceOf(DataIntegrityViolationException.class)
        .isInstanceOfSatisfying(
            DataIntegrityViolationException.class,
            exception -> {
              Throwable cause = exception.getCause();
              ConstraintViolationException violation = null;

              while (cause != null) {
                if (cause instanceof ConstraintViolationException) {
                  violation = (ConstraintViolationException) cause;
                  assertThat(violation.getConstraintName()).isEqualTo("uk_guardian_email_lower");
                  break;
                }
                cause = cause.getCause();
              }

              assertThat(violation).isNotNull();
            });
  }

  @DisplayName("[M1-JPA-02] savesRefreshTokenAndFindsItByHashWithGuardianAssociation")
  @Test
  void savesRefreshTokenAndFindsItByHashWithGuardianAssociation() {
    Guardian guardian = guardianRepository.saveAndFlush(newGuardian("token@example.com"));
    String tokenHash = "a".repeat(64);
    Instant now = Instant.parse("2026-08-26T00:00:00Z");
    Instant expiresAt = now.plusSeconds(60);
    Instant revokedAt = now.plusSeconds(30);
    refreshTokenRepository.saveAndFlush(
        new RefreshToken(UUID.randomUUID(), guardian, tokenHash, expiresAt, revokedAt, now));
    entityManager.clear();

    RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash).orElseThrow();

    assertThat(refreshToken.getTokenHash()).isEqualTo(tokenHash);
    assertThat(refreshToken.getGuardian().getId()).isEqualTo(guardian.getId());
    assertThat(refreshToken.getCreatedAt()).isEqualTo(now);
    assertThat(refreshToken.getExpiresAt()).isEqualTo(expiresAt);
    assertThat(refreshToken.getRevokedAt()).isEqualTo(revokedAt);
  }

  @DisplayName("[M1-JPA-03] pessimisticWriteLockSerializesConcurrentAccessToTheSameToken")
  @Test
  void pessimisticWriteLockSerializesConcurrentAccessToTheSameToken() throws Exception {
    Guardian guardian = guardianRepository.saveAndFlush(newGuardian("lock@example.com"));
    String tokenHash = "b".repeat(64);
    Instant now = clock.instant();
    refreshTokenRepository.saveAndFlush(
        new RefreshToken(UUID.randomUUID(), guardian, tokenHash, now.plusSeconds(60), null, now));

    CompletableFuture<Integer> firstPid = new CompletableFuture<>();
    CompletableFuture<Integer> secondPid = new CompletableFuture<>();
    CountDownLatch firstLockAcquired = new CountDownLatch(1);
    CountDownLatch releaseFirstTransaction = new CountDownLatch(1);

    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      try {
        var firstTransaction =
            executor.submit(
                () ->
                    transactionTemplate.executeWithoutResult(
                        status -> {
                          firstPid.complete(currentBackendPid());
                          refreshTokenRepository.findByTokenHashWithPessimisticWriteLock(tokenHash);
                          firstLockAcquired.countDown();
                          await(releaseFirstTransaction);
                        }));

        await(firstLockAcquired);

        var secondTransaction =
            executor.submit(
                () ->
                    transactionTemplate.executeWithoutResult(
                        status -> {
                          secondPid.complete(currentBackendPid());
                          refreshTokenRepository.findByTokenHashWithPessimisticWriteLock(tokenHash);
                        }));

        int blockerPid = firstPid.get(5, TimeUnit.SECONDS);
        int waitingPid = secondPid.get(5, TimeUnit.SECONDS);
        assertBlockedBy(waitingPid, blockerPid);

        releaseFirstTransaction.countDown();
        firstTransaction.get(5, TimeUnit.SECONDS);
        secondTransaction.get(5, TimeUnit.SECONDS);
      } finally {
        // executor.close()가 작업 종료를 기다리기 전에 첫 transaction을 해제한다.
        releaseFirstTransaction.countDown();
      }
    }
  }

  private int currentBackendPid() {
    return jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Integer.class);
  }

  private void assertBlockedBy(int waitingPid, int blockerPid) throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
    do {
      Boolean blocked =
          jdbcTemplate.queryForObject(
              "SELECT ? = ANY(pg_blocking_pids(?))", Boolean.class, blockerPid, waitingPid);
      if (Boolean.TRUE.equals(blocked)) {
        return;
      }
      Thread.sleep(20);
    } while (System.nanoTime() < deadline);

    throw new AssertionError(
        "PID %d did not wait for a lock held by PID %d".formatted(waitingPid, blockerPid));
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
        DiscoveryStatus.SEARCHING,
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
