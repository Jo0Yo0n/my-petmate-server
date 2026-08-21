package io.github.jo0yo0n.mypetmate.guardian.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
    @NotNull(message = "필수입니다.") @Size(min = 20, max = 4096, message = "20자 이상 4096자 이하이어야 합니다.")
        String refreshToken) {}
