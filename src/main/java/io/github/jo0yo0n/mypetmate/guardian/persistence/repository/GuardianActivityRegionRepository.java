package io.github.jo0yo0n.mypetmate.guardian.persistence.repository;

import io.github.jo0yo0n.mypetmate.guardian.persistence.entity.GuardianActivityRegion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianActivityRegionRepository
    extends JpaRepository<GuardianActivityRegion, UUID> {

  @EntityGraph(attributePaths = {"region", "region.parent", "region.parent.parent"})
  List<GuardianActivityRegion> findAllByGuardianId(UUID guardianId);
}
