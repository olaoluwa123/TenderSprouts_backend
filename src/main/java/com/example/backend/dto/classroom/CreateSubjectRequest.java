package com.example.backend.dto.classroom;

import jakarta.validation.constraints.NotBlank;

public record CreateSubjectRequest(@NotBlank String name) {
}
