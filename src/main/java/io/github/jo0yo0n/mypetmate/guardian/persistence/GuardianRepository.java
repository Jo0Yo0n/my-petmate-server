package io.github.jo0yo0n.mypetmate.guardian.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianRepository extends JpaRepository<Guardian, UUID> {

  Optional<Guardian> findByEmail(String email);

  boolean existsByEmail(String email);
}
