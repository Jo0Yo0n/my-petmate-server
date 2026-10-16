package io.github.jo0yo0n.mypetmate.guardian;

import io.github.jo0yo0n.mypetmate.guardian.dto.GuardianResponse;
import io.github.jo0yo0n.mypetmate.guardian.dto.GuardianUpdateRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guardians/me")
class GuardianController {
  private final GuardianService guardianService;

  GuardianController(GuardianService guardianService) {
    this.guardianService = guardianService;
  }

  @GetMapping
  GuardianResponse getCurrentGuardian(@AuthenticationPrincipal UUID guardianId) {
    return guardianService.getCurrentGuardian(guardianId);
  }

  @PatchMapping
  GuardianResponse updateCurrentGuardian(
      @AuthenticationPrincipal UUID guardianId, @Valid @RequestBody GuardianUpdateRequest request) {
    return guardianService.updateCurrentGuardian(guardianId, request);
  }
}
