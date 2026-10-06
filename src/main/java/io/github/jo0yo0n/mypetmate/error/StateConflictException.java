package io.github.jo0yo0n.mypetmate.error;

public class StateConflictException extends BusinessException {

  public StateConflictException() {
    super(ErrorCode.STATE_CONFLICT, "요청한 상태에서 처리할 수 없습니다.");
  }

  public StateConflictException(String detail) {
    super(ErrorCode.STATE_CONFLICT, detail);
  }
}
