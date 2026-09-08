package io.github.jo0yo0n.mypetmate.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class JwtPropertiesTest {

  private static final String VALID_SECRET = "test-jwt-secret-that-is-at-least-32-bytes";
  private static final String VALID_ISSUER = "my-petmate-server";
  private static final String VALID_AUDIENCE = "my-petmate-api";

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(JwtPropertiesConfiguration.class);

  @DisplayName("[M1-CRYPTO-05] startsWithASecretOfAtLeast32Bytes")
  @Test
  void startsWithASecretOfAtLeast32Bytes() {
    contextRunner
        .withPropertyValues(
            "app.jwt.secret=" + VALID_SECRET,
            "app.jwt.issuer=" + VALID_ISSUER,
            "app.jwt.audience=" + VALID_AUDIENCE)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context.getBean(JwtProperties.class).secret()).isEqualTo(VALID_SECRET);
              assertThat(context.getBean(JwtProperties.class).issuer()).isEqualTo(VALID_ISSUER);
              assertThat(context.getBean(JwtProperties.class).audience()).isEqualTo(VALID_AUDIENCE);
            });
  }

  @DisplayName("[M1-CRYPTO-05] failsToStartWhenTheSecretIsMissing")
  @Test
  void failsToStartWhenTheSecretIsMissing() {
    contextRunner
        .withPropertyValues("app.jwt.issuer=" + VALID_ISSUER, "app.jwt.audience=" + VALID_AUDIENCE)
        .run(context -> assertThat(context).hasFailed());
  }

  @DisplayName("[M1-CRYPTO-05] failsToStartWhenTheSecretIsShorterThan32Bytes")
  @Test
  void failsToStartWhenTheSecretIsShorterThan32Bytes() {
    String shortSecret = "too-short";

    contextRunner
        .withPropertyValues(
            "app.jwt.secret=" + shortSecret,
            "app.jwt.issuer=" + VALID_ISSUER,
            "app.jwt.audience=" + VALID_AUDIENCE)
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertFailureDoesNotExpose(context.getStartupFailure(), shortSecret);
            });
  }

  private void assertFailureDoesNotExpose(Throwable failure, String secret) {
    for (Throwable exception = failure; exception != null; exception = exception.getCause()) {
      assertThat(String.valueOf(exception.getMessage())).doesNotContain(secret);
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(JwtProperties.class)
  static class JwtPropertiesConfiguration {}
}
