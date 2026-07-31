package com.example.backend.dto.parent;

import java.util.List;

public record ChildDashboardSummary(
    Long studentId,
    String studentName,
    String admissionNumber,
    List<ParentDashboardGradeItem> latestGrades
) {
}
