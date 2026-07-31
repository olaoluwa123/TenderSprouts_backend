package com.example.backend.dto.classroom;

import jakarta.validation.constraints.NotNull;

public record AssignClassSubjectRequest(@NotNull Long subjectId) {
}
