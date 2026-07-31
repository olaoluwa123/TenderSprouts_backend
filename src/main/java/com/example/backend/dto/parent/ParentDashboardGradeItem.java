package com.example.backend.dto.parent;

import com.example.backend.entity.Grade;
import com.example.backend.entity.Subject;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ParentDashboardGradeItem(
    Long id,
    Long subjectId,
    String subjectName,
    Long termId,
    String assessmentType,
    BigDecimal score,
    BigDecimal maxScore,
    LocalDateTime recordedAt
) {
    public static ParentDashboardGradeItem from(Grade grade, Subject subject) {
        LocalDateTime recordedAt = grade.getUpdatedAt() != null
            ? grade.getUpdatedAt()
            : grade.getCreatedAt();
        return new ParentDashboardGradeItem(
            grade.getId(),
            grade.getSubjectId(),
            subject != null ? subject.getName() : null,
            grade.getTermId(),
            grade.getAssessmentType(),
            grade.getScore(),
            grade.getMaxScore(),
            recordedAt);
    }
}
