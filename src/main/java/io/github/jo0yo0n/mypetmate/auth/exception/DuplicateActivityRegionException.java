package io.github.jo0yo0n.mypetmate.auth.exception;

import io.github.jo0yo0n.mypetmate.error.BusinessException;
import io.github.jo0yo0n.mypetmate.error.ErrorCode;

public class DuplicateActivityRegionException extends BusinessException {
  public DuplicateActivityRegionException() {
    super(ErrorCode.VALIDATION_FAILED, "같은 활동 지역을 중복으로 선택할 수 없습니다.");
  }

  public DuplicateActivityRegionException(String detail) {
    super(ErrorCode.VALIDATION_FAILED, detail);
  }
}
