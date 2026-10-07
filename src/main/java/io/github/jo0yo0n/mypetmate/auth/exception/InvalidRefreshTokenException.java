package io.github.jo0yo0n.mypetmate.auth.exception;

import io.github.jo0yo0n.mypetmate.error.BusinessException;
import io.github.jo0yo0n.mypetmate.error.ErrorCode;

public class InvalidRefreshTokenException extends BusinessException {
  public InvalidRefreshTokenException() {
    super(ErrorCode.AUTH_REFRESH_INVALID, "refresh token이 만료되었거나 유효하지 않습니다.");
  }

  public InvalidRefreshTokenException(String detail) {
    super(ErrorCode.AUTH_REFRESH_INVALID, detail);
  }
}
