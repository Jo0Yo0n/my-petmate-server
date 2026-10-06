package io.github.jo0yo0n.mypetmate.web.error;

import io.github.jo0yo0n.mypetmate.error.ErrorCode;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailFactory {

  public static ProblemDetail create(
      ErrorCode errorCode, String detail, URI instance, String requestId) {

    ProblemDetail problemDetail = ProblemDetail.forStatus(errorCode.getStatus());
    problemDetail.setType(errorCode.getType());
    problemDetail.setTitle(errorCode.getTitle());
    problemDetail.setDetail(detail);
    problemDetail.setInstance(instance);

    problemDetail.setProperty("code", errorCode.getCode());
    problemDetail.setProperty("requestId", requestId);

    return problemDetail;
  }

  public static ProblemDetail create(
      ErrorCode errorCode,
      String detail,
      URI instance,
      String requestId,
      List<ApiFieldError> fieldErrors) {

    Objects.requireNonNull(fieldErrors, "fieldErrors");

    ProblemDetail problemDetail = create(errorCode, detail, instance, requestId);

    if (!fieldErrors.isEmpty()) {
      problemDetail.setProperty("fieldErrors", fieldErrors);
      return problemDetail;
    }

    return problemDetail;
  }
}
