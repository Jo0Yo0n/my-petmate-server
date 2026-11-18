package io.github.jo0yo0n.mypetmate.auth.exception;

import io.github.jo0yo0n.mypetmate.error.BusinessException;
import io.github.jo0yo0n.mypetmate.error.ErrorCode;

public class NicknameAlreadyExistsException extends BusinessException {
  public NicknameAlreadyExistsException() {
    super(ErrorCode.NICKNAME_ALREADY_EXISTS, "이미 사용 중인 닉네임입니다.");
  }
}
