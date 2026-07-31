package com.example.backend.dto.grade;

import java.math.BigDecimal;

public record GradingSheetCellResponse(
    String assessmentType,
    BigDecimal score,
    BigDecimal maxScore
) {
}
