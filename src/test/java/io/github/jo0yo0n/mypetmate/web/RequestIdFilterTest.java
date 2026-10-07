package io.github.jo0yo0n.mypetmate.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.jo0yo0n.mypetmate.error.ErrorCode;
import io.github.jo0yo0n.mypetmate.web.error.ProblemDetailFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class RequestIdFilterTest {

  private static final String CLIENT_REQUEST_ID = "req-01ARZ3NDEKTSV4RRFFQ69G5FAV";

  RequestIdFilter requestIdFilter;
  MockHttpServletRequest request;
  MockHttpServletResponse response;

  @BeforeEach
  void setup() {
    requestIdFilter = new RequestIdFilter();
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    request.addHeader("My-Petmate-Request-Id", CLIENT_REQUEST_ID);
  }

  @AfterEach
  void clearMDC() {
    MDC.clear();
  }

  @DisplayName("[M1-REQ-02] generatesServerRequestIdIgnoringIncomingRequestIdHeader")
  @Test
  void generatesServerRequestIdIgnoringIncomingRequestIdHeader() throws Exception {

    FilterChain chain =
        (req, res) -> {
          String requestId = (String) req.getAttribute("requestId");

          assertThat(requestId).matches("^req-[0-7][0-9A-HJKMNP-TV-Z]{25}$");
          assertThat(requestId).isNotEqualTo(CLIENT_REQUEST_ID);
          assertThat(MDC.get("requestId")).isEqualTo(requestId);
          assertThat(((HttpServletResponse) response).getHeader("My-Petmate-Request-id"))
              .isEqualTo(requestId);
        };

    requestIdFilter.doFilter(request, response, chain);
  }

  @DisplayName("[M1-REQ-03] requestIdMatchesAcrossAttributeMdcHeaderAndProblemDetail")
  @Test
  void requestIdMatchesAcrossAttributeMdcHeaderAndProblemDetail() throws Exception {

    FilterChain chain =
        (req, res) -> {
          ProblemDetail problemDetail =
              ProblemDetailFactory.create(
                  ErrorCode.NOT_ACCEPTABLE,
                  "not acceptable",
                  URI.create("/test/request-id"),
                  (String) req.getAttribute("requestId"));

          assertThat(req.getAttribute("requestId")).isNotNull();
          assertThat(req.getAttribute("requestId")).isEqualTo(MDC.get("requestId"));
          assertThat(req.getAttribute("requestId"))
              .isEqualTo(((HttpServletResponse) res).getHeader("My-Petmate-Request-Id"));
          assertThat(req.getAttribute("requestId"))
              .isEqualTo(problemDetail.getProperties().get("requestId"));
        };

    requestIdFilter.doFilter(request, response, chain);
  }

  @DisplayName("[M1-REQ-04A] restoreMDCValueAfterChain")
  @Test
  void restoreMDCValueAfterChain() throws Exception {

    MDC.put("requestId", "alreadyExistsValue");

    FilterChain chain =
        (req, res) -> {
          String requestId = (String) req.getAttribute("requestId");
          assertThat(MDC.get("requestId")).isEqualTo(requestId);
        };

    requestIdFilter.doFilter(request, response, chain);

    assertThat(MDC.get("requestId")).isEqualTo("alreadyExistsValue");
  }

  @DisplayName("[M1-REQ-04B] removeMDCValueAfterChain")
  @Test
  void removeMDCValueAfterChain() throws Exception {

    FilterChain chain =
        (req, res) -> {
          String requestId = (String) req.getAttribute("requestId");
          assertThat(MDC.get("requestId")).isEqualTo(requestId);
        };

    requestIdFilter.doFilter(request, response, chain);

    assertThat(MDC.get("requestId")).isNull();
  }

  @DisplayName("[M1-REQ-04C] restoreMDCValueWhileFilterThrowsException")
  @Test
  void restoreMDCValueWhileFilterThrowsException() {

    MDC.put("requestId", "alreadyExistsValue");

    FilterChain chain =
        (req, res) -> {
          throw new ServletException();
        };

    assertThatThrownBy(() -> requestIdFilter.doFilter(request, response, chain))
        .isInstanceOf(ServletException.class);

    assertThat(MDC.get("requestId")).isEqualTo("alreadyExistsValue");
  }

  @DisplayName("[M1-REQ-04D] removeMDCValueWhileFilterThrowsException")
  @Test
  void removeMDCValueWhileFilterThrowsException() throws Exception {

    FilterChain chain =
        (req, res) -> {
          throw new ServletException();
        };

    assertThatThrownBy(() -> requestIdFilter.doFilter(request, response, chain))
        .isInstanceOf(ServletException.class);

    assertThat(MDC.get("requestId")).isNull();
  }
}
