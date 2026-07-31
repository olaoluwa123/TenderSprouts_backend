package com.example.backend.dto.dashboard;

import java.math.BigDecimal;

public record ClassPerformanceItem(
    Long classId,
    String className,
    BigDecimal averageScore,
    long studentCount
) {
}
