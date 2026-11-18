package io.github.jo0yo0n.mypetmate.guardian;

import io.github.jo0yo0n.mypetmate.guardian.dto.AvailableTimeSlotRequest;
import io.github.jo0yo0n.mypetmate.guardian.dto.GuardianResponse;
import io.github.jo0yo0n.mypetmate.guardian.dto.GuardianUpdateRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guardians/me")
class GuardianController {
  private static final Logger log = LoggerFactory.getLogger(GuardianController.class);

  private final GuardianService guardianService;

  GuardianController(GuardianService guardianService) {
    this.guardianService = guardianService;
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  GuardianResponse getCurrentGuardian(@AuthenticationPrincipal UUID guardianId) {
    return guardianService.getCurrentGuardian(guardianId);
  }

  @PatchMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  GuardianResponse updateCurrentGuardian(
      @AuthenticationPrincipal UUID guardianId, @Valid @RequestBody GuardianUpdateRequest request) {
    var command =
        new GuardianUpdateCommand(
            request.nickname(),
            request.profileType(),
            request.gender(),
            request.discoveryStatus(),
            request.activityRegionIds(),
            AvailableTimeSlotRequest.toActivityTimeSlots(request.availableTimeSlots()));
    return guardianService.updateCurrentGuardian(guardianId, command);
  }

  @PostMapping("/withdraw")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void withdrawCurrentGuardian(@AuthenticationPrincipal UUID guardianId) {
    guardianService.withdrawCurrentGuardian(guardianId);
    log.info(
        "event=GUARDIAN_WITHDRAWN guardianId={} requestId={}", guardianId, MDC.get("requestId"));
  }
}
