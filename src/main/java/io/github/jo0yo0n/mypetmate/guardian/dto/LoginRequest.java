package io.github.jo0yo0n.mypetmate.guardian.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        @Size(min = 3, max = 254, message = "3자 이상 254자 이하이어야 합니다.")
        String email,
    @NotNull(message = "필수입니다.") @Size(min = 1, max = 72, message = "1자 이상 72자 이하이어야 합니다.")
        String password) {}
