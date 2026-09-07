package io.github.jo0yo0n.mypetmate.guardian.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    int expiresIn,
    int refreshExpiresIn,
    GuardianResponse guardian) {}
