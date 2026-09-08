package io.github.jo0yo0n.mypetmate.auth.dto;

import io.github.jo0yo0n.mypetmate.guardian.dto.GuardianResponse;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    int expiresIn,
    int refreshExpiresIn,
    GuardianResponse guardian) {}
