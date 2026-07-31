package com.example.backend.dto.grade;

import com.example.backend.entity.Grade;
import java.math.BigDecimal;

public record GradeResponse(
    Long id,
    Long studentId,
    Long subjectId,
    Long teacherId,
    Long classId,
    Long sessionId,
    Long termId,
    String assessmentType,
    BigDecimal score,
    BigDecimal maxScore,
    String remarks
) {
    public static GradeResponse from(Grade grade) {
        return new GradeResponse(
            grade.getId(),
            grade.getStudentId(),
            grade.getSubjectId(),
            grade.getTeacherId(),
            grade.getClassId(),
            grade.getSessionId(),
            grade.getTermId(),
            grade.getAssessmentType(),
            grade.getScore(),
            grade.getMaxScore(),
            grade.getRemarks());
    }
}
