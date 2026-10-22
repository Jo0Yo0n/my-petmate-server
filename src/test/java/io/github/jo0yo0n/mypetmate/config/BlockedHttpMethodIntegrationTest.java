package io.github.jo0yo0n.mypetmate.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.jo0yo0n.mypetmate.support.PostgreSqlIntegrationTestSupport;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(BlockedHttpMethodIntegrationTest.ProbeConfig.class)
class BlockedHttpMethodIntegrationTest extends PostgreSqlIntegrationTestSupport {
  private static final String PATH = "/test/blocked-http-method";

  @LocalServerPort int port;
  @Autowired TestRestTemplate http;
  @Autowired ProbeController probe;

  static Stream<Arguments> blockedMethods() {
    return Stream.of("TRACE", "CONNECT", "PROPFIND", "MKCOL", "QUERY", "FOO")
        .flatMap(method -> Stream.of(Arguments.of(method, false), Arguments.of(method, true)));
  }

  @ParameterizedTest(name = "{0}, authenticated={1}")
  @MethodSource("blockedMethods")
  @DisplayName("[M1-SEC-11] rejectsGloballyBlockedMethodsBeforeEndpointExecution")
  void rejectsGloballyBlockedMethodsBeforeEndpointExecution(String method, boolean authenticated)
      throws Exception {
    String token = signup();
    probe.calls.set(0);
    // Prove this endpoint is reachable with the same real token and production security chain.
    assertThat(send("GET", token)).isEqualTo(200);
    assertThat(probe.calls.get()).isEqualTo(1);

    int status = send(method, authenticated ? token : null);

    // Different layers may reject the request. CONNECT may be rejected as not implemented.
    assertThat((status >= 400 && status < 500) || status == 501)
        .as("%s authenticated=%s must be rejected, received %s", method, authenticated, status)
        .isTrue();
    assertThat(probe.calls.get()).isEqualTo(1);
  }

  private String signup() {
    var response =
        http.postForEntity(
            "/api/auth/signup",
            Map.of(
                "email", "blocked-method@example.com",
                "password", "StrongPass123!",
                "profileType", "family",
                "discoveryStatus", "searching"),
            JsonNode.class);
    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getBody()).isNotNull();
    return response.getBody().path("accessToken").asText();
  }

  private int send(String method, String token) throws Exception {
    // Raw HTTP also supports CONNECT and arbitrary methods without client-side restrictions.
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
      socket.setSoTimeout(5000);
      String authority = "localhost:" + port;
      String target = method.equals("CONNECT") ? authority : PATH;
      String request =
          method
              + " "
              + target
              + " HTTP/1.1\r\nHost: "
              + authority
              + "\r\nConnection: close\r\n"
              + (token == null ? "" : "Authorization: Bearer " + token + "\r\n")
              + "\r\n";
      socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
      socket.getOutputStream().flush();
      String statusLine =
          new BufferedReader(
                  new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))
              .readLine();
      assertThat(statusLine).isNotNull().startsWith("HTTP/1.1 ");
      return Integer.parseInt(statusLine.split(" ")[1]);
    }
  }

  @TestConfiguration
  @Import(ProbeController.class)
  static class ProbeConfig {}

  @RestController
  static class ProbeController {
    final AtomicInteger calls = new AtomicInteger();

    // No method restriction: MVC routing must not mask a failure of global method blocking.
    @RequestMapping(PATH)
    String execute() {
      calls.incrementAndGet();
      return "executed";
    }
  }
}
