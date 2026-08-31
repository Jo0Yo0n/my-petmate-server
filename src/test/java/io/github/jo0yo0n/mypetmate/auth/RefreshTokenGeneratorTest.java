package io.github.jo0yo0n.mypetmate.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RefreshTokenGeneratorTest {

  private final RefreshTokenGenerator refreshTokenGenerator = new RefreshTokenGenerator();

  @Test
  void generatesUniqueOpaqueBase64UrlTokensWith256BitsOfEntropy() {
    String firstToken = refreshTokenGenerator.generate();
    String secondToken = refreshTokenGenerator.generate();

    assertThat(firstToken).matches("[A-Za-z0-9_-]{43}");
    assertThat(secondToken).matches("[A-Za-z0-9_-]{43}");
    assertThat(secondToken).isNotEqualTo(firstToken);
  }

  @Test
  void createsADeterministicSha256HashForDatabaseStorage() {
    String refreshToken = "A".repeat(43);

    String firstHash = refreshTokenGenerator.hash(refreshToken);
    String secondHash = refreshTokenGenerator.hash(refreshToken);

    assertThat(firstHash).isEqualTo(secondHash).matches("[0-9a-f]{64}");
    assertThat(firstHash).isNotEqualTo(refreshToken);
  }
}
