package io.github.jo0yo0n.mypetmate.guardian.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailNormalizerTest {

  @Test
  void normalizesEmailByTrimmingAndLowercasing() {
    assertThat(EmailNormalizer.normalize("  Guardian@Example.COM  "))
        .isEqualTo("guardian@example.com");
  }

  @Test
  void preservesNullEmail() {
    assertThat(EmailNormalizer.normalize(null)).isNull();
  }
}
