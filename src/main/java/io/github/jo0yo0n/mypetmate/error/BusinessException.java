package io.github.jo0yo0n.mypetmate.error;

public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;
  private final String detail;

  public BusinessException(ErrorCode errorCode, String detail) {
    super(detail);
    this.errorCode = errorCode;
    this.detail = detail;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  public String getDetail() {
    return detail;
  }
}
