package io.github.jo0yo0n.mypetmate.auth.exception;

import io.github.jo0yo0n.mypetmate.guardian.domain.GuardianStatus;
import org.springframework.security.core.AuthenticationException;

public class AccountStatusAuthenticationException extends AuthenticationException {

  private final GuardianStatus status;

  public AccountStatusAuthenticationException(GuardianStatus status) {
    super("Account access denied");
    if (status != GuardianStatus.TEMPORARILY_RESTRICTED && status != GuardianStatus.WITHDRAWN) {
      throw new IllegalArgumentException("Account status must be restricted or withdrawn");
    }
    this.status = status;
  }

  public GuardianStatus getStatus() {
    return status;
  }
}
