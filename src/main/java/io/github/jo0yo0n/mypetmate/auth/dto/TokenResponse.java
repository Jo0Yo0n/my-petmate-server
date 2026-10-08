package io.github.jo0yo0n.mypetmate.auth.dto;

/** Service result containing tokens; map to the HTTP response body before serialization. */
public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    int expiresIn,
    int refreshExpiresIn) {}
