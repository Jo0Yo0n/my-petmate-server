package io.github.jo0yo0n.mypetmate.guardian.dto;

import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import io.github.jo0yo0n.mypetmate.guardian.domain.IdentityVisibility;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import io.github.jo0yo0n.mypetmate.guardian.validation.GuardianProfileFields;
import io.github.jo0yo0n.mypetmate.guardian.validation.ValidGuardianProfile;
import jakarta.validation.constraints.NotNull;

@ValidGuardianProfile
public record GuardianUpdateRequest(
    @NotNull(message = "필수입니다.") ProfileType profileType,
    Gender gender,
    @NotNull(message = "필수입니다.") IdentityVisibility identityVisibility)
    implements GuardianProfileFields {}
