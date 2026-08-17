package com.example.backend.dto.parent;

import java.math.BigDecimal;
import java.util.List;

public record ChildDashboardSummary(
    Long studentId,
    String studentName,
    String admissionNumber,
    String className,
    BigDecimal outstandingFees,
    List<ParentDashboardGradeItem> latestGrades
) {
}
