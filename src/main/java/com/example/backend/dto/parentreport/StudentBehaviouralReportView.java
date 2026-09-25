package com.example.backend.dto.parentreport;

import java.time.LocalDateTime;

public record StudentBehaviouralReportView(
    Long studentId,
    String studentName,
    String admissionNumber,
    String className,
    Long termId,
    String termName,
    String sessionName,
    String periodType,
    Integer weekNumber,
    String punctuality,
    String neatness,
    String cooperation,
    String responsibility,
    String conduct,
    String remarks,
    LocalDateTime publishedAt,
    String gender,
    Integer ageYears,
    String height,
    String weight,
    String classTeacherName,
    Integer daysSchoolOpened,
    Integer daysPresent,
    String reportDate
) {
}
