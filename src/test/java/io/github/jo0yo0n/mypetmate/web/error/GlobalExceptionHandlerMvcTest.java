package io.github.jo0yo0n.mypetmate.web.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jo0yo0n.mypetmate.auth.exception.EmailAlreadyExistsException;
import io.github.jo0yo0n.mypetmate.config.RequestIdConfig;
import io.github.jo0yo0n.mypetmate.error.ErrorCode;
import io.github.jo0yo0n.mypetmate.error.StateConflictException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@WebMvcTest(ErrorTestController.class)
@Import(RequestIdConfig.class)
class GlobalExceptionHandlerMvcTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @DisplayName("[M1-ERR-01A] mapsInvalidRequestBodyToValidationFailed")
  @Test
  @WithMockUser
  void mapsInvalidRequestBodyToValidationFailed() throws Exception {
    assertProblem(
        post("/test/validation")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"\"}"),
        "/test/validation",
        400,
        ErrorCode.VALIDATION_FAILED.getCode(),
        "name");
  }

  @DisplayName("[M1-ERR-01B] mapsInvalidMethodParameterToValidationFailed")
  @Test
  @WithMockUser
  void mapsInvalidMethodParameterToValidationFailed() throws Exception {
    assertProblem(
        get("/test/parameter-validation").param("count", "0"),
        "/test/parameter-validation",
        400,
        ErrorCode.VALIDATION_FAILED.getCode(),
        "count");
  }

  @DisplayName("[M1-ERR-01C] mapsMissingQueryParameterToValidationFailed")
  @Test
  @WithMockUser
  void mapsMissingQueryParameterToValidationFailed() throws Exception {
    MvcResult result =
        assertProblem(
            get("/test/parameter-validation").param("token", "private-query-token"),
            "/test/parameter-validation",
            400,
            ErrorCode.VALIDATION_FAILED.getCode(),
            "count");
    assertThat(result.getResponse().getContentAsString()).doesNotContain("private-query-token");
  }

  @DisplayName("[M1-ERR-01D] mapsMalformedJsonToMalformedRequest")
  @Test
  @WithMockUser
  void mapsMalformedJsonToMalformedRequest() throws Exception {
    assertProblem(
        post("/test/validation")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{"),
        "/test/validation",
        400,
        ErrorCode.MALFORMED_REQUEST.getCode());
  }

  @DisplayName("[M1-ERR-01E] mapsPathVariableTypeMismatchToValidationFailed")
  @Test
  @WithMockUser
  void mapsPathVariableTypeMismatchToValidationFailed() throws Exception {
    assertProblem(
        get("/test/type-mismatch/not-a-number"),
        "/test/type-mismatch/not-a-number",
        400,
        ErrorCode.VALIDATION_FAILED.getCode(),
        "number");
  }

  @DisplayName("[M1-ERR-02A] mapsNoHandlerFoundToResourceNotFound")
  @Test
  @WithMockUser
  void mapsNoHandlerFoundToResourceNotFound() throws Exception {
    assertProblem(
        get("/test/no-handler"), "/test/no-handler", 404, ErrorCode.RESOURCE_NOT_FOUND.getCode());
  }

  @DisplayName("[M1-ERR-02B] mapsNoResourceFoundToResourceNotFound")
  @Test
  @WithMockUser
  void mapsNoResourceFoundToResourceNotFound() throws Exception {
    assertProblem(
        get("/test/no-resource"), "/test/no-resource", 404, ErrorCode.RESOURCE_NOT_FOUND.getCode());
  }

  @DisplayName("[M1-ERR-02C] mapsStateConflictBusinessException")
  @Test
  @WithMockUser
  void mapsStateConflictBusinessException() throws Exception {
    assertProblem(
        get("/test/state-conflict"),
        "/test/state-conflict",
        409,
        ErrorCode.STATE_CONFLICT.getCode());
  }

  @DisplayName("[M1-ERR-02D] mapsEmailAlreadyExistsException")
  @Test
  @WithMockUser
  void mapsEmailAlreadyExistsException() throws Exception {
    assertProblem(
        post("/test/duplicated-email")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{ \"email\": \"test@example.com\" }"),
        "/test/duplicated-email",
        409,
        ErrorCode.EMAIL_ALREADY_EXISTS.getCode());
  }

  @DisplayName("[M1-ERR-03A] mapsInvalidReturnValueToInternalServerError")
  @Test
  @WithMockUser
  void mapsInvalidReturnValueToInternalServerError() throws Exception {
    MvcResult result =
        assertProblem(
            get("/test/invalid-return-value"),
            "/test/invalid-return-value",
            500,
            ErrorCode.INTERNAL_SERVER_ERROR.getCode());
    assertInternalInformationAbsent(result);
  }

  @DisplayName("[M1-ERR-03B] mapsUnexpectedExceptionToInternalServerError")
  @Test
  @WithMockUser
  void mapsUnexpectedExceptionToInternalServerError() throws Exception {
    MvcResult result =
        assertProblem(
            get("/test/unexpected"),
            "/test/unexpected",
            500,
            ErrorCode.INTERNAL_SERVER_ERROR.getCode());
    assertInternalInformationAbsent(result);
  }

  @DisplayName("[M1-ERR-04A] mapsUnsupportedMethodToMethodNotAllowed")
  @Test
  @WithMockUser
  void mapsUnsupportedMethodToMethodNotAllowed() throws Exception {
    assertProblem(
        put("/test/state-conflict").with(SecurityMockMvcRequestPostProcessors.csrf()),
        "/test/state-conflict",
        405,
        ErrorCode.METHOD_NOT_ALLOWED.getCode());
  }

  @DisplayName("[M1-ERR-04B] mapsUnsupportedAcceptHeaderToNotAcceptable")
  @Test
  @WithMockUser
  void mapsUnsupportedAcceptHeaderToNotAcceptable() throws Exception {
    assertProblem(
        get("/test/json-only").accept(MediaType.APPLICATION_XML),
        "/test/json-only",
        406,
        ErrorCode.NOT_ACCEPTABLE.getCode());
  }

  @DisplayName("[M1-ERR-05] mapsUnsupportedContentTypeToUnsupportedMediaType")
  @Test
  @WithMockUser
  void mapsUnsupportedContentTypeToUnsupportedMediaType() throws Exception {
    assertProblem(
        post("/test/validation")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.TEXT_PLAIN),
        "/test/validation",
        415,
        ErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode());
  }

  private MvcResult assertProblem(
      MockHttpServletRequestBuilder request, String path, int expectedStatus, String expectedCode)
      throws Exception {
    return assertProblem(request, path, expectedStatus, expectedCode, new String[0]);
  }

  private MvcResult assertProblem(
      MockHttpServletRequestBuilder request,
      String path,
      int expectedStatus,
      String expectedCode,
      String... expectedFields)
      throws Exception {

    ErrorCode errorCode = ErrorCode.valueOf(expectedCode);

    MvcResult result =
        mockMvc
            .perform(request)
            .andExpect(status().is(expectedStatus))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(expectedStatus))
            .andExpect(jsonPath("$.code").value(expectedCode))
            .andExpect(jsonPath("$.type").value(errorCode.getType().toString()))
            .andExpect(jsonPath("$.title").value(errorCode.getTitle()))
            .andExpect(jsonPath("$.detail").isString())
            .andExpect(jsonPath("$.instance").value(path))
            .andReturn();

    JsonNode problemDetail = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(problemDetail.get("detail").asText()).isNotBlank();
    if (expectedFields.length == 0) {
      assertThat(problemDetail.has("fieldErrors")).isFalse();
    } else {
      JsonNode fieldErrors = problemDetail.path("fieldErrors");
      assertThat(fieldErrors.isArray()).isTrue();
      assertThat(fieldErrors.size()).isEqualTo(expectedFields.length);
      for (int i = 0; i < expectedFields.length; i++) {
        assertThat(fieldErrors.get(i).path("field").asText()).isEqualTo(expectedFields[i]);
        assertThat(fieldErrors.get(i).path("reason").asText()).isNotBlank();
      }
    }
    assertRequestIdMatchesResponse(result, path);
    return result;
  }

  private void assertInternalInformationAbsent(MvcResult result) throws Exception {
    String body = result.getResponse().getContentAsString();
    JsonNode problemDetail = objectMapper.readTree(body);
    assertThat(problemDetail.properties())
        .extracting(entry -> entry.getKey())
        .containsExactlyInAnyOrder(
            "type", "title", "status", "detail", "instance", "code", "requestId");
    assertThat(body)
        .doesNotContain(
            "IllegalStateException",
            "HandlerMethodValidationException",
            ErrorTestController.INTERNAL_MESSAGE,
            ErrorTestController.INTERNAL_SQL,
            ErrorTestController.PASSWORD,
            ErrorTestController.TOKEN);
  }

  private void assertRequestIdMatchesResponse(MvcResult result, String path) throws Exception {
    String requestIdHeader = result.getResponse().getHeader("My-Petmate-Request-Id");
    JsonNode problemDetail = objectMapper.readTree(result.getResponse().getContentAsString());

    assertThat(requestIdHeader).matches("^req-[0-7][0-9A-HJKMNP-TV-Z]{25}$");
    assertThat(problemDetail.get("instance").asText()).isEqualTo(path);
    assertThat(problemDetail.get("requestId").asText()).isEqualTo(requestIdHeader);
  }
}

@RestController
class ErrorTestController {

  static final String INTERNAL_MESSAGE = "fixture internal failure";
  static final String INTERNAL_SQL = "SELECT password_hash FROM guardian";
  static final String PASSWORD = "fixture-password";
  static final String TOKEN = "fixture-token";

  record TestRequest(@NotBlank String name) {}

  record EmailRequest(@NotBlank String email) {}

  @GetMapping("/test/no-handler")
  void noHandler() throws NoHandlerFoundException {
    throw new NoHandlerFoundException("GET", "/test/no-handler", HttpHeaders.EMPTY);
  }

  @GetMapping("/test/no-resource")
  void noResource() throws NoResourceFoundException {
    throw new NoResourceFoundException(HttpMethod.GET, "/test/no-resource");
  }

  @GetMapping("/test/state-conflict")
  void stateConflict() {
    throw new StateConflictException();
  }

  @PostMapping("/test/validation")
  void validation(@Valid @RequestBody TestRequest request) {}

  @PostMapping("/test/duplicated-email")
  void duplicatedEmail(@Valid @RequestBody EmailRequest request) {
    throw new EmailAlreadyExistsException();
  }

  @GetMapping("/test/parameter-validation")
  void parameterValidation(@RequestParam @Min(1) int count) {}

  @GetMapping("/test/invalid-return-value")
  @NotNull
  String invalidReturnValue() {
    return null;
  }

  @GetMapping("/test/type-mismatch/{number}")
  void typeMismatch(@PathVariable int number) {}

  @GetMapping(value = "/test/json-only", produces = MediaType.APPLICATION_JSON_VALUE)
  String jsonOnly() {
    return "ok";
  }

  @GetMapping("/test/unexpected")
  void unexpected() {
    throw new IllegalStateException(
        INTERNAL_MESSAGE + "; " + INTERNAL_SQL + "; password=" + PASSWORD + "; token=" + TOKEN);
  }
}
