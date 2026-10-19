package io.github.jo0yo0n.mypetmate.guardian.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  @Query(
      "select refreshToken.guardian.id from RefreshToken refreshToken where refreshToken.tokenHash = :tokenHash")
  Optional<UUID> findGuardianIdByTokenHash(@Param("tokenHash") String tokenHash);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select refreshToken
      from RefreshToken refreshToken
      where refreshToken.tokenHash = :tokenHash
      """)
  Optional<RefreshToken> findByTokenHashWithPessimisticWriteLock(
      @Param("tokenHash") String tokenHash);

  @Modifying
  @Query(
      """
      update RefreshToken refreshToken
      set refreshToken.revokedAt = :revokedAt
      where refreshToken.guardian.id = :guardianId and refreshToken.revokedAt is null
      """)
  int revokeAllByGuardianId(
      @Param("guardianId") UUID guardianId, @Param("revokedAt") Instant revokedAt);

  void deleteByTokenHash(String tokenHash);
}
