package io.github.jo0yo0n.mypetmate.error;

public class UnexpectedFailureException extends RuntimeException {

  public UnexpectedFailureException(String message) {
    super(message);
  }

  public UnexpectedFailureException() {
    super("unexpected error occurred");
  }
}
