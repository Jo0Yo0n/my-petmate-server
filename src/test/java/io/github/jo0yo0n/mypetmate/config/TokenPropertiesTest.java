package io.github.jo0yo0n.mypetmate.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class TokenPropertiesTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(TokenPropertiesConfiguration.class);

  @Test
  void bindsTheTokenPolicy() {
    contextRunner
        .withPropertyValues(
            "app.token.access-token-ttl=900s",
            "app.token.refresh-token-ttl=2592000s",
            "app.token.token-type=Bearer")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              TokenProperties tokenProperties = context.getBean(TokenProperties.class);
              assertThat(tokenProperties.accessTokenTtl()).isEqualTo(Duration.ofSeconds(900));
              assertThat(tokenProperties.refreshTokenTtl()).isEqualTo(Duration.ofDays(30));
              assertThat(tokenProperties.tokenType()).isEqualTo("Bearer");
            });
  }

  @Test
  void failsToStartWhenTheAccessTokenTtlIsZero() {
    contextRunner
        .withPropertyValues(
            "app.token.access-token-ttl=0s",
            "app.token.refresh-token-ttl=2592000s",
            "app.token.token-type=Bearer")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void failsToStartWhenTheRefreshTokenTtlIsNegative() {
    contextRunner
        .withPropertyValues(
            "app.token.access-token-ttl=900s",
            "app.token.refresh-token-ttl=-1s",
            "app.token.token-type=Bearer")
        .run(context -> assertThat(context).hasFailed());
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(TokenProperties.class)
  static class TokenPropertiesConfiguration {}
}
