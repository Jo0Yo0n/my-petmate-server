package io.github.jo0yo0n.mypetmate.auth.exception;

import io.github.jo0yo0n.mypetmate.error.BusinessException;
import io.github.jo0yo0n.mypetmate.error.ErrorCode;

public class EmailAlreadyExistsException extends BusinessException {
  public EmailAlreadyExistsException() {
    super(ErrorCode.EMAIL_ALREADY_EXISTS, "이미 사용 중인 이메일입니다.");
  }

  public EmailAlreadyExistsException(String detail) {
    super(ErrorCode.EMAIL_ALREADY_EXISTS, detail);
  }
}
