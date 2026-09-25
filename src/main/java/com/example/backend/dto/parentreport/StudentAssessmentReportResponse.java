package com.example.backend.dto.parentreport;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record StudentAssessmentReportResponse(
    Long studentId,
    String studentName,
    String admissionNumber,
    String className,
    Long termId,
    String termName,
    String sessionName,
    String type,
    Integer weekNumber,
    LocalDateTime publishedAt,
    BigDecimal averageScore,
    String gender,
    Integer ageYears,
    String height,
    String weight,
    String classTeacherName,
    Integer daysSchoolOpened,
    Integer daysPresent,
    String reportDate,
    List<ScoreType> types,
    List<SubjectRow> subjects
) {
    public record ScoreType(
        String code,
        String name,
        BigDecimal maxScore
    ) {
    }

    public record SubjectRow(
        String name,
        Map<String, BigDecimal> scores
    ) {
    }
}
