package io.github.jo0yo0n.mypetmate.auth.exception;

import io.github.jo0yo0n.mypetmate.error.BusinessException;
import io.github.jo0yo0n.mypetmate.error.ErrorCode;

public class AccountRestrictedException extends BusinessException {
  public AccountRestrictedException() {
    super(ErrorCode.ACCOUNT_RESTRICTED, "일시 정지된 계정은 이용할 수 없습니다.");
  }

  public AccountRestrictedException(String message) {
    super(ErrorCode.ACCOUNT_RESTRICTED, message);
  }
}
