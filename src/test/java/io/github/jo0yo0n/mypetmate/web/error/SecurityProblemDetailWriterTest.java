package io.github.jo0yo0n.mypetmate.web.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jo0yo0n.mypetmate.error.ErrorCode;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletResponse;

@JsonTest
class SecurityProblemDetailWriterTest {

  @Autowired ObjectMapper objectMapper;
  SecurityProblemDetailWriter writer;

  @BeforeEach
  void setUp() {
    writer = new SecurityProblemDetailWriter(objectMapper);
  }

  @DisplayName("[M1-WRITER-01] writesProblemDetailToHttpResponse")
  @Test
  void writesProblemDetailToHttpResponse() throws Exception {

    MockHttpServletResponse response = new MockHttpServletResponse();

    String requestId = "request-id";
    ProblemDetail problemDetail =
        ProblemDetailFactory.create(
            ErrorCode.VALIDATION_FAILED,
            "validation failed",
            URI.create("/test/validation"),
            requestId,
            List.of(new ApiFieldError("name", "too long")));

    writer.write(problemDetail, response);

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

    JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
    assertThat(body.get("requestId").asText()).isEqualTo(requestId);
    assertThat(body.get("code").asText()).isEqualTo(ErrorCode.VALIDATION_FAILED.getCode());
    assertThat(body.get("status").asInt()).isEqualTo(400);
    assertThat(body.get("title").asText()).isEqualTo(problemDetail.getTitle());
    assertThat(body.get("detail").asText()).isEqualTo(problemDetail.getDetail());
    assertThat(body.get("type").asText())
        .isEqualTo(ErrorCode.VALIDATION_FAILED.getType().toString());
    assertThat(body.get("instance").asText()).isEqualTo("/test/validation");

    JsonNode fieldErrors = body.path("fieldErrors");
    assertThat(fieldErrors.isArray()).isTrue();
    assertThat(fieldErrors.size()).isEqualTo(1);
    assertThat(fieldErrors.get(0).path("field").asText()).isEqualTo("name");
    assertThat(fieldErrors.get(0).path("reason").asText()).isEqualTo("too long");
  }

  @DisplayName("[M1-WRITER-01]omitsFieldErrorsWhenThereAreNoFieldErrors")
  @Test
  void omitsFieldErrorsWhenThereAreNoFieldErrors() throws Exception {
    ProblemDetail problemDetail =
        ProblemDetailFactory.create(
            ErrorCode.AUTH_TOKEN_INVALID, "invalid token", URI.create("/test/auth"), "request-id");

    MockHttpServletResponse response = new MockHttpServletResponse();

    writer.write(problemDetail, response);

    JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
    assertThat(body.has("fieldErrors")).isFalse();
  }
}
