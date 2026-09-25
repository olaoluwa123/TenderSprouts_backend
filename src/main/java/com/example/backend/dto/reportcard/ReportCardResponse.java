package com.example.backend.dto.reportcard;

import java.math.BigDecimal;
import java.util.List;

public record ReportCardResponse(
    Long studentId,
    String studentName,
    String admissionNumber,
    Long termId,
    String termName,
    Long sessionId,
    String sessionName,
    Long classId,
    String className,
    BigDecimal averageScore,
    String letterGrade,
    Integer rankInClass,
    String comments,
    List<ReportCardSubjectSummary> subjectSummaries,
    List<ReportCardSubjectGrade> subjectGrades,
    String gender,
    Integer ageYears,
    String classTeacherName,
    Integer daysSchoolOpened,
    Integer daysPresent,
    String reportDate,
    String nextTermBegins,
    BigDecimal totalScore,
    String performanceBand,
    String height,
    String weight,
    String attitudeComment,
    String areasToImprove
) {
}
