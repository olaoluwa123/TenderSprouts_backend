package com.example.backend.dto.teacher;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OnboardTeacherRequest(
    @NotBlank @Email String email,
    @NotBlank String fullName,
    String phone
) {
}
