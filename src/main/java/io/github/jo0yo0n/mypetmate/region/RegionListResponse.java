package io.github.jo0yo0n.mypetmate.region;

import io.github.jo0yo0n.mypetmate.region.domain.RegionLevel;
import java.util.List;
import java.util.UUID;

record RegionListResponse(List<Item> items) {
  record Item(UUID id, String name, RegionLevel level, UUID parentId) {}
}
