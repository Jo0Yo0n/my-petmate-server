package io.github.jo0yo0n.mypetmate.web.error;

import io.github.jo0yo0n.mypetmate.error.ErrorCode;
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

    boolean expired = isExpirationOnly(authException);
    ErrorCode code = expired ? ErrorCode.AUTH_TOKEN_EXPIRED : ErrorCode.AUTH_TOKEN_INVALID;

    String detail = expired ? "access token이 만료되었습니다." : "유효한 access token이 필요합니다.";

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
