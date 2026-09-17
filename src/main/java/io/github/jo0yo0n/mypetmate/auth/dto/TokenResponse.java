package io.github.jo0yo0n.mypetmate.auth.dto;

public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    int expiresIn,
    int refreshExpiresIn) {}
