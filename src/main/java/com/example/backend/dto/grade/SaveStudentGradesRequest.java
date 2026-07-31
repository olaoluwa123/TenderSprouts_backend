package com.example.backend.dto.grade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record SaveStudentGradesRequest(
    @NotNull Long termId,
    @NotNull Long classId,
    @NotEmpty @Valid List<GradeEntryRequest> entries
) {
    public record GradeEntryRequest(
        @NotNull Long subjectId,
        @NotBlank String assessmentType,
        BigDecimal score
    ) {
    }
}
