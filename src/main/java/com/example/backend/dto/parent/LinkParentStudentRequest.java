package com.example.backend.dto.parent;

import jakarta.validation.constraints.NotNull;

public record LinkParentStudentRequest(@NotNull Long studentId) {
}
