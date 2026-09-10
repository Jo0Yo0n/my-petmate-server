package io.github.jo0yo0n.mypetmate.guardian.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailNormalizerTest {

  @DisplayName("[M1-DTO-04] normalizesEmailByTrimmingAndLowercasing")
  @Test
  void normalizesEmailByTrimmingAndLowercasing() {
    assertThat(EmailNormalizer.normalize("  Guardian@Example.COM  "))
        .isEqualTo("guardian@example.com");
  }

  @DisplayName("[M1-DTO-04] preservesNullEmail")
  @Test
  void preservesNullEmail() {
    assertThat(EmailNormalizer.normalize(null)).isNull();
  }
}
