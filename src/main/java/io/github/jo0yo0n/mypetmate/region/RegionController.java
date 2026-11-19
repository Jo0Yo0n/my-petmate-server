package io.github.jo0yo0n.mypetmate.region;

import io.github.jo0yo0n.mypetmate.error.FieldValidationException;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/regions")
class RegionController {
  private final RegionService regionService;

  RegionController(RegionService regionService) {
    this.regionService = regionService;
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  RegionListResponse listChildRegions(@RequestParam(required = false) String parentId) {
    UUID id = null;
    if (parentId != null) {
      try {
        id = UUID.fromString(parentId);
        if (!id.toString().equalsIgnoreCase(parentId)) {
          throw new FieldValidationException("parentId", "올바른 UUID 형식으로 입력해 주세요.");
        }
      } catch (IllegalArgumentException exception) {
        throw new FieldValidationException("parentId", "올바른 UUID 형식으로 입력해 주세요.");
      }
    }
    return regionService.listOfChildren(id);
  }
}
