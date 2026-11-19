package io.github.jo0yo0n.mypetmate.region;

import io.github.jo0yo0n.mypetmate.error.NotFoundException;
import io.github.jo0yo0n.mypetmate.region.domain.RegionLevel;
import io.github.jo0yo0n.mypetmate.region.persistence.repository.RegionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RegionService {
  private final RegionRepository regionRepository;

  RegionService(RegionRepository regionRepository) {
    this.regionRepository = regionRepository;
  }

  @Transactional(readOnly = true)
  RegionListResponse listOfChildren(UUID parentId) {
    if (parentId != null) {
      var parent =
          regionRepository
              .findById(parentId)
              .orElseThrow(() -> new NotFoundException("지역을 찾을 수 없습니다."));
      if (parent.getLevel() == RegionLevel.UMD) {
        return new RegionListResponse(List.of());
      }
    }
    var regions =
        parentId == null
            ? regionRepository.findAllByParentIsNullOrderByNameAscIdAsc()
            : regionRepository.findAllByParentIdOrderByNameAscIdAsc(parentId);
    return new RegionListResponse(
        regions.stream()
            .map(
                region ->
                    new RegionListResponse.Item(
                        region.getId(), region.getName(), region.getLevel(), parentId))
            .toList());
  }
}
