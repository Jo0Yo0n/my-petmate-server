package io.github.jo0yo0n.mypetmate.web.error;

import io.github.jo0yo0n.mypetmate.error.ErrorCode;
import io.github.jo0yo0n.mypetmate.error.UnexpectedFailureException;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.slf4j.Logger;

public final class UnexpectedErrorLogger {

  private UnexpectedErrorLogger() {}

  public static void log(Logger logger, Exception exception, String requestId) {

    String failureMessage =
        exception instanceof UnexpectedFailureException ? exception.getMessage() : null;
    String stackTrace =
        Arrays.stream(exception.getStackTrace())
            .map(StackTraceElement::toString)
            .collect(Collectors.joining("\n    at "));

    logger.error(
        """
        Unexpected server error
          code={}
          requestId={}
          exceptionType={}
          failureMessage={}
          stackTrace=
            at {}
        """,
        ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
        requestId,
        exception.getClass().getName(),
        failureMessage,
        stackTrace);
  }
}
