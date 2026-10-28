package com.example.backend.dto.preschool;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PublishPreschoolReportRequest(
    @NotNull Long classId,
    @NotNull Long termId,
    @Min(0) @Max(4) Integer monthNumber
) {
}
