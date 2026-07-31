package com.example.backend.dto.classroom;

import jakarta.validation.constraints.NotBlank;

public record CreateSchoolClassRequest(@NotBlank String name) {
}
