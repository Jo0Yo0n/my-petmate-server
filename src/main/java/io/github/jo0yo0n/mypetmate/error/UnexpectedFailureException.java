package io.github.jo0yo0n.mypetmate.error;

public class UnexpectedFailureException extends RuntimeException {

  public UnexpectedFailureException(String message) {
    super(message);
  }

  public UnexpectedFailureException() {
    super("예기치 않은 오류가 발생했습니다.");
  }
}
