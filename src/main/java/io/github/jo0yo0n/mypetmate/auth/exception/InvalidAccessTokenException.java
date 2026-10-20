package io.github.jo0yo0n.mypetmate.auth.exception;

import io.github.jo0yo0n.mypetmate.error.BusinessException;
import io.github.jo0yo0n.mypetmate.error.ErrorCode;

public class InvalidAccessTokenException extends BusinessException {

  public InvalidAccessTokenException() {
    super(ErrorCode.AUTH_TOKEN_INVALID, "유효한 access token이 필요합니다.");
  }

  public InvalidAccessTokenException(String message) {
    super(ErrorCode.AUTH_TOKEN_INVALID, message);
  }
}
