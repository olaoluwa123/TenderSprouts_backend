package com.example.backend.dto.user;

import com.example.backend.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
    @NotBlank @Email String email,
    @NotBlank String fullName,
    @NotNull User.Role role,
    String phone
) {
}
