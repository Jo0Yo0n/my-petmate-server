package io.github.jo0yo0n.mypetmate.guardian.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import io.github.jo0yo0n.mypetmate.guardian.domain.GuardianStatus;
import io.github.jo0yo0n.mypetmate.guardian.domain.IdentityVisibility;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import io.github.jo0yo0n.mypetmate.guardian.persistence.Guardian;
import java.util.UUID;

public record GuardianResponse(
    UUID id,
    String email,
    ProfileType profileType,
    @JsonInclude(Include.NON_NULL) Gender gender,
    IdentityVisibility identityVisibility,
    GuardianStatus status) {

  public static GuardianResponse from(Guardian guardian) {
    return new GuardianResponse(
        guardian.getId(),
        guardian.getEmail(),
        guardian.getProfileType(),
        guardian.getGender(),
        guardian.getIdentityVisibility(),
        guardian.getStatus());
  }
}
