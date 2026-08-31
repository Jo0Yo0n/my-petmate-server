package io.github.jo0yo0n.mypetmate.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
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
            });
  }

  private PropertySource<?> loadYaml(String name, String path) throws IOException {
    return new YamlPropertySourceLoader().load(name, new FileSystemResource(path)).getFirst();
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
