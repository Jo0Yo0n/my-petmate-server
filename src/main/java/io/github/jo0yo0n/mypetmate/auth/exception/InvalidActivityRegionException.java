package io.github.jo0yo0n.mypetmate.auth.exception;

import io.github.jo0yo0n.mypetmate.error.BusinessException;
import io.github.jo0yo0n.mypetmate.error.ErrorCode;

public class InvalidActivityRegionException extends BusinessException {
  public InvalidActivityRegionException() {
    super(ErrorCode.VALIDATION_FAILED, "존재하지 않는 읍·면·동 입니다.");
  }

  public InvalidActivityRegionException(String detail) {
    super(ErrorCode.VALIDATION_FAILED, detail);
  }
}
