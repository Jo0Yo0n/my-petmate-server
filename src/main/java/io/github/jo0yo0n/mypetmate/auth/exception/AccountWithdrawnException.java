package io.github.jo0yo0n.mypetmate.auth.exception;

import io.github.jo0yo0n.mypetmate.error.BusinessException;
import io.github.jo0yo0n.mypetmate.error.ErrorCode;

public class AccountWithdrawnException extends BusinessException {
  public AccountWithdrawnException() {
    super(ErrorCode.ACCOUNT_WITHDRAWN, "탈퇴한 계정은 이용할 수 없습니다.");
  }

  public AccountWithdrawnException(String message) {
    super(ErrorCode.ACCOUNT_WITHDRAWN, message);
  }
}
