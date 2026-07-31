package com.example.backend.dto.dashboard;

import java.math.BigDecimal;

public record StudentPerformanceItem(
    Long studentId,
    String studentName,
    String admissionNumber,
    BigDecimal averageScore,
    Integer rankInClass,
    String letterGrade
) {
}
