package io.github.jo0yo0n.mypetmate.web;

import io.github.jo0yo0n.mypetmate.error.ErrorCode;
import io.github.jo0yo0n.mypetmate.web.error.ProblemDetailFactory;
import io.github.jo0yo0n.mypetmate.web.error.SecurityProblemDetailWriter;
import io.github.jo0yo0n.mypetmate.web.error.UnexpectedErrorLogger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ProblemDetail;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.filter.OncePerRequestFilter;

public class DataAccessExceptionFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(DataAccessExceptionFilter.class);

  private final SecurityProblemDetailWriter writer;

  public DataAccessExceptionFilter(SecurityProblemDetailWriter writer) {
    this.writer = writer;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    try {
      filterChain.doFilter(request, response);
    } catch (DataAccessException
        | CannotCreateTransactionException
        | TransactionSystemException exception) {

      ProblemDetail problemDetail =
          ProblemDetailFactory.create(
              ErrorCode.INTERNAL_SERVER_ERROR,
              "요청을 처리하는 중 오류가 발생했습니다.",
              URI.create(request.getRequestURI()),
              (String) request.getAttribute("requestId"));

      UnexpectedErrorLogger.log(log, exception, (String) request.getAttribute("requestId"));

      writer.write(problemDetail, response);
    }
  }
}
