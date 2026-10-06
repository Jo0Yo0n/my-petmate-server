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
import io.github.jo0yo0n.mypetmate.config.RequestIdConfig;
import io.github.jo0yo0n.mypetmate.error.ErrorCode;
import io.github.jo0yo0n.mypetmate.error.StateConflictException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

  @Test
  @WithMockUser
  void mapsNoHandlerFoundToResourceNotFound() throws Exception {
    assertProblem("/test/no-handler", 404, ErrorCode.RESOURCE_NOT_FOUND.getCode());
  }

  @Test
  @WithMockUser
  void mapsNoResourceFoundToResourceNotFound() throws Exception {
    assertProblem("/test/no-resource", 404, ErrorCode.RESOURCE_NOT_FOUND.getCode());
  }

  @Test
  @WithMockUser
  void mapsStateConflictBusinessException() throws Exception {
    assertProblem("/test/state-conflict", 409, ErrorCode.STATE_CONFLICT.getCode());
  }

  @Test
  @WithMockUser
  void mapsInvalidRequestBodyToValidationFailed() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/test/validation")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.getCode()))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
            .andReturn();

    assertRequestIdMatchesResponse(result, "/test/validation");
  }

  @Test
  @WithMockUser
  void mapsInvalidMethodParameterToValidationFailed() throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/test/parameter-validation").param("count", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.getCode()))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("count"))
            .andReturn();

    assertRequestIdMatchesResponse(result, "/test/parameter-validation");
  }

  @Test
  @WithMockUser
  void mapsMissingQueryParameterToValidationFailed() throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/test/parameter-validation").param("token", "private-query-token"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("count"))
            .andReturn();

    assertRequestIdMatchesResponse(result, "/test/parameter-validation");
  }

  @Test
  @WithMockUser
  void mapsUnsupportedContentTypeToUnsupportedMediaType() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/test/validation")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.TEXT_PLAIN))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
            .andExpect(jsonPath("$.instance").value("/test/validation"))
            .andExpect(jsonPath("$.fieldErrors").doesNotExist())
            .andReturn();

    assertRequestIdMatchesResponse(result, "/test/validation");
  }

  @Test
  @WithMockUser
  void mapsInvalidReturnValueToInternalServerError() throws Exception {
    assertProblem("/test/invalid-return-value", 500, ErrorCode.INTERNAL_SERVER_ERROR.getCode());
  }

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

  @Test
  @WithMockUser
  void mapsPathVariableTypeMismatchToValidationFailed() throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/test/type-mismatch/not-a-number"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.getCode()))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("number"))
            .andReturn();

    assertRequestIdMatchesResponse(result, "/test/type-mismatch/not-a-number");
  }

  @Test
  @WithMockUser
  void mapsUnsupportedMethodToMethodNotAllowed() throws Exception {
    assertProblem(
        put("/test/state-conflict").with(SecurityMockMvcRequestPostProcessors.csrf()),
        "/test/state-conflict",
        405,
        ErrorCode.METHOD_NOT_ALLOWED.getCode());
  }

  @Test
  @WithMockUser
  void mapsUnsupportedAcceptHeaderToNotAcceptable() throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/test/json-only").accept(MediaType.APPLICATION_XML))
            .andExpect(status().isNotAcceptable())
            .andExpect(jsonPath("$.code").value(ErrorCode.NOT_ACCEPTABLE.getCode()))
            .andExpect(jsonPath("$.fieldErrors").doesNotExist())
            .andReturn();

    assertRequestIdMatchesResponse(result, "/test/json-only");
  }

  @Test
  @WithMockUser
  void mapsUnexpectedExceptionToInternalServerError() throws Exception {
    assertProblem("/test/unexpected", 500, ErrorCode.INTERNAL_SERVER_ERROR.getCode());
  }

  private void assertProblem(String path, int expectedStatus, String expectedCode)
      throws Exception {

    assertProblem(get(path), path, expectedStatus, expectedCode);
  }

  private void assertProblem(
      MockHttpServletRequestBuilder request, String path, int expectedStatus, String expectedCode)
      throws Exception {

    MvcResult result =
        mockMvc
            .perform(request)
            .andExpect(status().is(expectedStatus))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(expectedStatus))
            .andExpect(jsonPath("$.code").value(expectedCode))
            .andExpect(jsonPath("$.instance").value(path))
            .andExpect(jsonPath("$.fieldErrors").doesNotExist())
            .andReturn();

    assertRequestIdMatchesResponse(result, path);
  }

  private void assertRequestIdMatchesResponse(MvcResult result, String path) throws Exception {
    String requestIdHeader = result.getResponse().getHeader("My-Petmate-Request-Id");
    JsonNode problemDetail = objectMapper.readTree(result.getResponse().getContentAsString());

    assertThat(requestIdHeader).isNotNull();
    assertThat(problemDetail.get("instance").asText()).isEqualTo(path);
    assertThat(problemDetail.get("requestId").asText()).isEqualTo(requestIdHeader);
  }
}

@RestController
class ErrorTestController {

  record TestRequest(@NotBlank String name) {}

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
    throw new IllegalStateException("test exception");
  }
}
