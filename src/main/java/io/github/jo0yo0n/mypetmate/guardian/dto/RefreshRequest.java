package io.github.jo0yo0n.mypetmate.guardian.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
    @NotNull(message = "필수입니다.")
        @Size(min = 43, max = 43, message = "43자이어야 합니다.")
        @Pattern(regexp = "^[A-Za-z0-9_-]{43}$", message = "Base64 URL 형식이어야 합니다.")
        String refreshToken) {}
