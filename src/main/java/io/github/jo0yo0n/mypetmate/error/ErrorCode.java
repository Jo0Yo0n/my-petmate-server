package io.github.jo0yo0n.mypetmate.error;

import java.net.URI;
import org.springframework.http.HttpStatus;

public enum ErrorCode {
  VALIDATION_FAILED(
      URI.create("urn:my-petmate:problem:validation-failed"), "입력값 검증 실패", HttpStatus.BAD_REQUEST),
  MALFORMED_REQUEST(
      URI.create("urn:my-petmate:problem:malformed-request"), "잘못된 요청 형식", HttpStatus.BAD_REQUEST),
  AUTH_TOKEN_INVALID(
      URI.create("urn:my-petmate:problem:auth-token-invalid"),
      "유효하지 않은 access token",
      HttpStatus.UNAUTHORIZED),
  AUTH_REFRESH_INVALID(
      URI.create("urn:my-petmate:problem:refresh-token-invalid"),
      "유효하지 않은 refresh token",
      HttpStatus.UNAUTHORIZED),
  AUTH_INVALID_CREDENTIALS(
      URI.create("urn:my-petmate:problem:invalid-credentials"),
      "유효하지 않은 로그인 정보",
      HttpStatus.UNAUTHORIZED),
  EMAIL_ALREADY_EXISTS(
      URI.create("urn:my-petmate:problem:email-already-exists"), "이미 가입된 이메일", HttpStatus.CONFLICT),
  RESOURCE_NOT_FOUND(
      URI.create("urn:my-petmate:problem:resource-not-found"),
      "리소스를 찾을 수 없음",
      HttpStatus.NOT_FOUND),
  STATE_CONFLICT(URI.create("urn:my-petmate:problem:state-conflict"), "상태 충돌", HttpStatus.CONFLICT),
  METHOD_NOT_ALLOWED(
      URI.create("urn:my-petmate:problem:method-not-allowed"),
      "지원하지 않는 HTTP 메서드",
      HttpStatus.METHOD_NOT_ALLOWED),
  UNSUPPORTED_MEDIA_TYPE(
      URI.create("urn:my-petmate:problem:unsupported-media-type"),
      "지원하지 않는 미디어 형식",
      HttpStatus.UNSUPPORTED_MEDIA_TYPE),
  NOT_ACCEPTABLE(
      URI.create("urn:my-petmate:problem:not-acceptable"),
      "지원하지 않는 응답 형식",
      HttpStatus.NOT_ACCEPTABLE),
  INTERNAL_SERVER_ERROR(
      URI.create("urn:my-petmate:problem:internal-server-error"),
      "서버 내부 오류",
      HttpStatus.INTERNAL_SERVER_ERROR);

  private final URI type;
  private final String title;
  private final HttpStatus status;

  ErrorCode(URI type, String title, HttpStatus status) {
    this.type = type;
    this.title = title;
    this.status = status;
  }

  public URI getType() {
    return type;
  }

  public String getTitle() {
    return title;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getCode() {
    return name();
  }
}
