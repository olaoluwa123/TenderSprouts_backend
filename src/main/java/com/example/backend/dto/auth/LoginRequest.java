package com.example.backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password
) {
}
