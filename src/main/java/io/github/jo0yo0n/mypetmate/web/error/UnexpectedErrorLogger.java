package io.github.jo0yo0n.mypetmate.web.error;

import io.github.jo0yo0n.mypetmate.error.ErrorCode;
import java.util.Arrays;
import org.slf4j.Logger;

public final class UnexpectedErrorLogger {

  private UnexpectedErrorLogger() {}

  public static void log(Logger logger, Exception exception, String requestId) {
    logger.error(
        "event=unexpected_error code={} requestId={} exceptionType={} stackTrace={}",
        ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
        requestId,
        exception.getClass().getName(),
        Arrays.toString(exception.getStackTrace()));
  }
}
