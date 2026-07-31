package com.example.backend.dto.grade;

import com.example.backend.entity.AssessmentTypeLookup;
import java.math.BigDecimal;

public record GradingSheetAssessmentTypeResponse(
    String code,
    String name,
    BigDecimal defaultMaxScore
) {
    public static GradingSheetAssessmentTypeResponse from(AssessmentTypeLookup type) {
        return new GradingSheetAssessmentTypeResponse(
            type.getCode(),
            type.getName(),
            type.getDefaultMaxScore());
    }
}
