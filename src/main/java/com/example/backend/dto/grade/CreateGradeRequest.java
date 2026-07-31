package com.example.backend.dto.grade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateGradeRequest(
    @NotNull Long studentId,
    @NotNull Long subjectId,
    @NotNull Long classId,
    @NotNull Long termId,
    @NotBlank String assessmentType,
    BigDecimal score,
    String remarks
) {
}
