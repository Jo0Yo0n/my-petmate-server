package io.github.jo0yo0n.mypetmate.error;

public final class NotFoundException extends BusinessException {
  public NotFoundException(String detail) {
    super(ErrorCode.RESOURCE_NOT_FOUND, detail);
  }
}
