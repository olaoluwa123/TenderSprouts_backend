package com.example.backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
    @NotBlank @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String currentPassword,
    @NotBlank @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String newPassword
) {
}
