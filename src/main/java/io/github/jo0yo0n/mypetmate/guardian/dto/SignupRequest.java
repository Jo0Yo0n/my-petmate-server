package io.github.jo0yo0n.mypetmate.guardian.dto;

import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import io.github.jo0yo0n.mypetmate.guardian.domain.IdentityVisibility;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;
import io.github.jo0yo0n.mypetmate.guardian.support.EmailNormalizer;
import io.github.jo0yo0n.mypetmate.guardian.validation.GuardianProfileFields;
import io.github.jo0yo0n.mypetmate.guardian.validation.ValidGuardianProfile;
import io.github.jo0yo0n.mypetmate.guardian.validation.ValidSignupPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@ValidGuardianProfile
public record SignupRequest(
    @NotBlank(message = "필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        @Size(min = 3, max = 254, message = "3자 이상 254자 이하이어야 합니다.")
        String email,
    @NotBlank(message = "필수입니다.")
        @Size(min = 8, max = 72, message = "8자 이상 72자 이하이어야 합니다.")
        @ValidSignupPassword
        String password,
    @NotNull(message = "필수입니다.") ProfileType profileType,
    Gender gender,
    @NotNull(message = "필수입니다.") IdentityVisibility identityVisibility)
    implements GuardianProfileFields {

  public SignupRequest {
    email = EmailNormalizer.normalize(email);
  }
}
