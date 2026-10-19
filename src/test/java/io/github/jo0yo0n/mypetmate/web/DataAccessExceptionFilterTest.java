package io.github.jo0yo0n.mypetmate.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jo0yo0n.mypetmate.web.error.SecurityProblemDetailWriter;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
class DataAccessExceptionFilterTest {
  private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
  private final DataAccessExceptionFilter filter =
      new DataAccessExceptionFilter(new SecurityProblemDetailWriter(mapper));

  @Test
  @DisplayName("[M1-DBFILTER-01] returnsSafeProblemDetailAndLogsDatabaseFailure")
  void returnsSafeProblemDetailAndLogsDatabaseFailure(CapturedOutput output) throws Exception {
    var request = new MockHttpServletRequest("GET", "/api/guardians/me");
    var response = new MockHttpServletResponse();
    String secret = "SELECT password_hash FROM guardian; password=private; token=private";
    new RequestIdFilter()
        .doFilter(
            request,
            response,
            (req, res) ->
                filter.doFilter(
                    req,
                    res,
                    (ignoredRequest, ignoredResponse) -> {
                      throw new DataAccessResourceFailureException(secret);
                    }));

    var body = mapper.readTree(response.getContentAsString());
    String requestId = response.getHeader("My-Petmate-Request-Id");
    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(response.getContentType()).startsWith("application/problem+json");
    assertThat(body.path("code").asText()).isEqualTo("INTERNAL_SERVER_ERROR");
    assertThat(body.path("status").asInt()).isEqualTo(500);
    assertThat(body.path("type").asText())
        .isEqualTo("urn:my-petmate:problem:internal-server-error");
    assertThat(body.path("title").asText()).isEqualTo("서버 내부 오류");
    assertThat(body.path("instance").asText()).isEqualTo("/api/guardians/me");
    assertThat(body.path("requestId").asText()).isEqualTo(requestId);
    assertThat(body.has("fieldErrors")).isFalse();
    assertThat(response.getContentAsString())
        .doesNotContain(secret, "DataAccessResourceFailureException");
    assertThat(output.getAll())
        .contains(
            "Unexpected server error",
            "code=INTERNAL_SERVER_ERROR",
            "requestId=" + requestId,
            "exceptionType=org.springframework.dao.DataAccessResourceFailureException",
            "failureMessage=null",
            "DataAccessExceptionFilterTest.")
        .doesNotContain(secret);
  }

  @Test
  @DisplayName("[M1-DBFILTER-02] preservesSuccessfulResponse")
  void preservesSuccessfulResponse() throws Exception {
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    filter.doFilter(
        request,
        response,
        (req, res) -> {
          response.setStatus(201);
          response.getWriter().write("success");
        });
    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(response.getContentAsString()).isEqualTo("success");
  }

  @Test
  @DisplayName("[M1-DBFILTER-03] propagatesUnrelatedException")
  void propagatesUnrelatedException() {
    var exception = new ServletException("unrelated");
    assertThatThrownBy(
            () ->
                filter.doFilter(
                    new MockHttpServletRequest(),
                    new MockHttpServletResponse(),
                    (req, res) -> {
                      throw exception;
                    }))
        .isSameAs(exception);
  }
}
