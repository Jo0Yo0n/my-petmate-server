package io.github.jo0yo0n.mypetmate.web.error;

import io.github.jo0yo0n.mypetmate.auth.exception.AccountStatusAuthenticationException;
import io.github.jo0yo0n.mypetmate.error.ErrorCode;
import io.github.jo0yo0n.mypetmate.guardian.domain.GuardianStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final SecurityProblemDetailWriter writer;

  public SecurityAuthenticationEntryPoint(SecurityProblemDetailWriter securityProblemDetailWriter) {
    this.writer = securityProblemDetailWriter;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    ErrorCode code;
    String detail;
    if (authException instanceof AccountStatusAuthenticationException accountException) {
      boolean withdrawn = accountException.getStatus() == GuardianStatus.WITHDRAWN;
      code = withdrawn ? ErrorCode.ACCOUNT_WITHDRAWN : ErrorCode.ACCOUNT_RESTRICTED;
      detail = withdrawn ? "탈퇴한 계정은 이용할 수 없습니다." : "일시 정지된 계정은 이용할 수 없습니다.";
    } else {
      boolean expired = isExpirationOnly(authException);
      code = expired ? ErrorCode.AUTH_TOKEN_EXPIRED : ErrorCode.AUTH_TOKEN_INVALID;
      detail = expired ? "access token이 만료되었습니다." : "유효한 access token이 필요합니다.";
    }

    ProblemDetail problem =
        ProblemDetailFactory.create(
            code,
            detail,
            URI.create(request.getRequestURI()),
            (String) request.getAttribute("requestId"));

    writer.write(problem, response);
  }

  private boolean isExpirationOnly(AuthenticationException exception) {
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {

      if (cause instanceof JwtValidationException validationException) {
        var errors = validationException.getErrors();

        return !errors.isEmpty()
            && errors.stream()
                .allMatch(error -> "access_token_expired".equals(error.getErrorCode()));
      }
    }

    return false;
  }
}
