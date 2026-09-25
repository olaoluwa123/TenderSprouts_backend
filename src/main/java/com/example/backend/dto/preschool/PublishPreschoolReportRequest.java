package com.example.backend.dto.preschool;

import jakarta.validation.constraints.NotNull;

public record PublishPreschoolReportRequest(
    @NotNull Long classId,
    @NotNull Long termId
) {
}
