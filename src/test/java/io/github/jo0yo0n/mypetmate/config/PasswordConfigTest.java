package io.github.jo0yo0n.mypetmate.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordConfigTest {

  @DisplayName("[M1-CRYPTO-01] encodesPassword")
  @Test
  void encodesPassword() {
    var passwordEncoder = new PasswordConfig().passwordEncoder();
    var rawPassword = "my-secret-password";
    var encodedPassword = passwordEncoder.encode(rawPassword);

    assertThat(encodedPassword).isNotEqualTo(rawPassword);
    assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
  }

  @DisplayName("[M1-CRYPTO-01] rejectsIncorrectPassword")
  @Test
  void rejectsIncorrectPassword() {
    var passwordEncoder = new PasswordConfig().passwordEncoder();
    var rawPassword = "my-secret-password";
    var encodedPassword = passwordEncoder.encode(rawPassword);

    assertThat(passwordEncoder.matches("incorrect-password", encodedPassword)).isFalse();
  }

  @DisplayName("[M1-CRYPTO-01] encodesSamePasswordDifferently")
  @Test
  void encodesSamePasswordDifferently() {
    var passwordEncoder = new PasswordConfig().passwordEncoder();
    var rawPassword = "my-secret-password";
    var encodedPassword1 = passwordEncoder.encode(rawPassword);
    var encodedPassword2 = passwordEncoder.encode(rawPassword);

    assertThat(encodedPassword1).isNotEqualTo(encodedPassword2);
  }

  @DisplayName("[M1-CRYPTO-01] doesNotIncludeRawPassword")
  @Test
  void doesNotIncludeRawPassword() {
    var passwordEncoder = new PasswordConfig().passwordEncoder();
    var rawPassword = "my-secret-password";
    var encodedPassword = passwordEncoder.encode(rawPassword);

    assertThat(encodedPassword).doesNotContain(rawPassword);
  }
}
