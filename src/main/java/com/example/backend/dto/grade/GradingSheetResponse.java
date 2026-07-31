package com.example.backend.dto.grade;

import java.util.List;

public record GradingSheetResponse(
    Long studentId,
    String studentName,
    Long classId,
    String className,
    Long termId,
    List<GradingSheetAssessmentTypeResponse> assessmentTypes,
    List<GradingSheetSubjectRowResponse> subjects,
    List<String> warnings
) {
}
