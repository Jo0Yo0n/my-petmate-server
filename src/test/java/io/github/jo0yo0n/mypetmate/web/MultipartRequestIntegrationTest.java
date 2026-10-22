package io.github.jo0yo0n.mypetmate.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.jo0yo0n.mypetmate.support.PostgreSqlIntegrationTestSupport;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MultipartRequestIntegrationTest extends PostgreSqlIntegrationTestSupport {
  @Autowired TestRestTemplate http;

  @ParameterizedTest
  @ValueSource(strings = {"signup", "login", "refresh", "logout", "profile"})
  void rejectsMultipartWithoutBoundaryForJsonEndpoints(String endpoint) {
    String token = signup();
    boolean profile = endpoint.equals("profile");
    String path = profile ? "/api/guardians/me" : "/api/auth/" + endpoint;
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    var response =
        http.exchange(
            path,
            profile ? HttpMethod.PATCH : HttpMethod.POST,
            new HttpEntity<>("{}", headers),
            JsonNode.class);

    assertThat(response.getStatusCode().value()).isEqualTo(415);
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    JsonNode body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.path("code").asText()).isEqualTo("UNSUPPORTED_MEDIA_TYPE");
    assertThat(body.path("requestId").asText())
        .isEqualTo(response.getHeaders().getFirst("My-Petmate-Request-Id"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"get", "withdraw"})
  void doesNotParseUnusedMultipartForBodylessEndpoints(String endpoint) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(signup());
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    boolean withdraw = endpoint.equals("withdraw");

    var response =
        http.exchange(
            "/api/guardians/me" + (withdraw ? "/withdraw" : ""),
            withdraw ? HttpMethod.POST : HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(withdraw ? 204 : 200);
  }

  private String signup() {
    var response =
        http.postForEntity(
            "/api/auth/signup",
            Map.of(
                "email", "multipart@example.com",
                "password", "StrongPass123!",
                "profileType", "family",
                "discoveryStatus", "searching"),
            JsonNode.class);
    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getBody()).isNotNull();
    return response.getBody().path("accessToken").asText();
  }
}
