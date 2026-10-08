package io.github.jo0yo0n.mypetmate.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

class TokenPolicyContractTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(TokenPropertiesConfiguration.class);

  @DisplayName("[M1-CRYPTO-06] applicationTokenPolicyAndResponseConstantsMatchTheOpenApiContract")
  @Test
  void applicationTokenPolicyAndResponseConstantsMatchTheOpenApiContract() throws IOException {
    PropertySource<?> application = loadYaml("application", "src/main/resources/application.yaml");
    PropertySource<?> openApi = loadYaml("openapi", "docs/openapi.yaml");

    contextRunner
        .withPropertyValues(
            "app.token.access-token-ttl=" + application.getProperty("app.token.access-token-ttl"),
            "app.token.refresh-token-ttl=" + application.getProperty("app.token.refresh-token-ttl"),
            "app.token.token-type=" + application.getProperty("app.token.token-type"))
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              TokenProperties tokenProperties = context.getBean(TokenProperties.class);

              assertThat(tokenProperties.accessTokenTtl())
                  .isEqualTo(Duration.ofSeconds(openApiSeconds(openApi, "accessTokenTtlSeconds")));
              assertThat(tokenProperties.refreshTokenTtl())
                  .isEqualTo(Duration.ofSeconds(openApiSeconds(openApi, "refreshTokenTtlSeconds")));
              assertThat(tokenProperties.tokenType())
                  .isEqualTo(openApiTokenPolicy(openApi, "tokenType"));
              assertResponseConstantsMatch(openApi, "TokenResponse", tokenProperties);
              assertResponseConstantsMatch(openApi, "AuthResponse", tokenProperties);
              assertTokenDeliveryMatchesContract(openApi, tokenProperties);
            });
  }

  private PropertySource<?> loadYaml(String name, String path) throws IOException {
    return new YamlPropertySourceLoader().load(name, new FileSystemResource(path)).getFirst();
  }

  private void assertTokenDeliveryMatchesContract(
      PropertySource<?> openApi, TokenProperties tokenProperties) {
    assertThat(openApiTokenPolicy(openApi, "accessTokenDelivery")).isEqualTo("json-body");
    assertThat(openApiTokenPolicy(openApi, "refreshTokenDelivery")).isEqualTo("httponly-cookie");
    assertThat(openApiTokenPolicy(openApi, "refreshTokenCookie.httpOnly")).isEqualTo(true);
    assertThat(openApiTokenPolicy(openApi, "refreshTokenCookie.maxAgeSeconds"))
        .isEqualTo(Math.toIntExact(tokenProperties.refreshTokenTtl().toSeconds()));
    for (String response : new String[] {"AuthResponse", "TokenResponse"}) {
      assertThat(
              openApi.getProperty(
                  "components.schemas." + response + ".properties.refreshToken.type"))
          .isNull();
    }
    assertThat(openApi.getProperty("components.schemas.RefreshRequest.type")).isNull();
    for (String endpoint : new String[] {"refresh", "logout"}) {
      String prefix = "paths./api/auth/" + endpoint + ".post.";
      assertThat(openApi.getProperty(prefix + "requestBody.required")).isNull();
      assertThat(openApi.getProperty(prefix + "parameters[0].$ref"))
          .isEqualTo("#/components/parameters/RefreshTokenCookie");
    }
    for (String[] success :
        new String[][] {
          {"signup", "201"}, {"login", "200"}, {"refresh", "200"}, {"logout", "204"}
        }) {
      assertThat(
              openApi.getProperty(
                  "paths./api/auth/"
                      + success[0]
                      + ".post.responses."
                      + success[1]
                      + ".headers.Set-Cookie.$ref"))
          .isEqualTo(
              "#/components/headers/"
                  + (success[0].equals("logout")
                      ? "RefreshTokenClearCookie"
                      : "RefreshTokenSetCookie"));
    }
  }

  private long openApiSeconds(PropertySource<?> openApi, String name) {
    Object value = openApiTokenPolicy(openApi, name);
    assertThat(value).isInstanceOf(Number.class);
    return ((Number) value).longValue();
  }

  private Object openApiTokenPolicy(PropertySource<?> openApi, String name) {
    return openApi.getProperty("info.x-token-policy." + name);
  }

  private void assertResponseConstantsMatch(
      PropertySource<?> openApi, String responseName, TokenProperties tokenProperties) {
    String prefix = "components.schemas." + responseName + ".properties.";
    assertThat(openApi.getProperty(prefix + "tokenType.const"))
        .isEqualTo(tokenProperties.tokenType());
    assertThat(openApi.getProperty(prefix + "expiresIn.const"))
        .isEqualTo(Math.toIntExact(tokenProperties.accessTokenTtl().toSeconds()));
    assertThat(openApi.getProperty(prefix + "refreshExpiresIn.const"))
        .isEqualTo(Math.toIntExact(tokenProperties.refreshTokenTtl().toSeconds()));
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(TokenProperties.class)
  static class TokenPropertiesConfiguration {}
}
