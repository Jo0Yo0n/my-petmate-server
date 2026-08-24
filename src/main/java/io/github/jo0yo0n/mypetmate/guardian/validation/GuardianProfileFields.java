package io.github.jo0yo0n.mypetmate.guardian.validation;

import io.github.jo0yo0n.mypetmate.guardian.domain.Gender;
import io.github.jo0yo0n.mypetmate.guardian.domain.ProfileType;

public interface GuardianProfileFields {

  ProfileType profileType();

  Gender gender();
}
