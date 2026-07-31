package com.example.backend.dto.reportcard;

import java.math.BigDecimal;

public record ReportCardSubjectGrade(
    Long subjectId,
    String subjectName,
    String assessmentType,
    BigDecimal score,
    BigDecimal maxScore,
    BigDecimal percentage
) {
}
