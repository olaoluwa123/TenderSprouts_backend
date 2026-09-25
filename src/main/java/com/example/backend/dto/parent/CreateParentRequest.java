package com.example.backend.dto.parent;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateParentRequest(
    @NotBlank @Email String email,
    @NotBlank String fullName,
    String phone,
    String address
) {
}
