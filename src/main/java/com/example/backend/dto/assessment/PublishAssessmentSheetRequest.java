package com.example.backend.dto.assessment;

import com.example.backend.entity.enums.AssessmentSheetType;
import jakarta.validation.constraints.NotNull;

public record PublishAssessmentSheetRequest(
    @NotNull AssessmentSheetType type,
    @NotNull Long classId,
    @NotNull Long termId,
    Integer weekNumber,
    Long studentId
) {
}
