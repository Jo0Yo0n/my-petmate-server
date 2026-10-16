package io.github.jo0yo0n.mypetmate.guardian.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuardianRepository extends JpaRepository<Guardian, UUID> {

  Optional<Guardian> findByEmail(String email);

  boolean existsByEmail(String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select guardian from Guardian guardian where guardian.id = :id")
  Optional<Guardian> findByIdWithPessimisticWriteLock(@Param("id") UUID id);
}
