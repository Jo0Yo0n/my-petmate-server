package io.github.jo0yo0n.mypetmate.region.persistence.repository;

import io.github.jo0yo0n.mypetmate.region.domain.RegionLevel;
import io.github.jo0yo0n.mypetmate.region.persistence.entity.Region;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegionRepository extends JpaRepository<Region, UUID> {

  @EntityGraph(attributePaths = {"parent", "parent.parent"})
  @Query(
      """
    select r
    from Region r
    where r.id in :ids
    and r.level = :level
""")
  List<Region> findSelectableLocalitiesByIdIn(
      @Param("ids") Collection<UUID> ids, @Param("level") RegionLevel level);
}
