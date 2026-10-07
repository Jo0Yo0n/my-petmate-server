package io.github.jo0yo0n.mypetmate.auth.exception;

import io.github.jo0yo0n.mypetmate.error.BusinessException;
import io.github.jo0yo0n.mypetmate.error.ErrorCode;

public class InvalidCredentialsException extends BusinessException {
  public InvalidCredentialsException() {
    super(ErrorCode.AUTH_INVALID_CREDENTIALS, "이메일 또는 비밀번호가 올바르지 않습니다.");
  }

  public InvalidCredentialsException(String detail) {
    super(ErrorCode.AUTH_INVALID_CREDENTIALS, detail);
  }
}
