package io.github.jo0yo0n.mypetmate.web.error;

import io.github.jo0yo0n.mypetmate.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class SecurityAccessDeniedHandler implements AccessDeniedHandler {

  private final SecurityProblemDetailWriter writer;

  public SecurityAccessDeniedHandler(SecurityProblemDetailWriter securityProblemDetailWriter) {
    this.writer = securityProblemDetailWriter;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {

    ProblemDetail problem =
        ProblemDetailFactory.create(
            ErrorCode.FORBIDDEN,
            "이 작업을 수행할 권한이 없습니다.",
            URI.create(request.getRequestURI()),
            (String) request.getAttribute("requestId"));

    writer.write(problem, response);
  }
}
