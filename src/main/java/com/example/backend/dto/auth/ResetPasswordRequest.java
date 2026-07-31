package com.example.backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank String token,
    @NotBlank @Size(min = 8, max = 100) @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String newPassword
) {
}
