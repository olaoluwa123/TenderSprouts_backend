package com.example.backend.dto.student;

import com.example.backend.entity.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record OnboardStudentWithParentRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    LocalDate dateOfBirth,
    @NotBlank String admissionNumber,
    Gender gender,
    @NotNull Long classId,
    @NotBlank @Email String parentEmail,
    @NotBlank String parentFullName,
    String parentPhone
) {
}
