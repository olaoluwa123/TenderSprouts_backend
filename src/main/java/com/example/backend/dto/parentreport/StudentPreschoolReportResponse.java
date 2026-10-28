package com.example.backend.dto.parentreport;

import java.time.LocalDateTime;
import java.util.List;

public record StudentPreschoolReportResponse(
    Long studentId,
    String studentName,
    String admissionNumber,
    String className,
    Long termId,
    String termName,
    Integer monthNumber,
    String sessionName,
    LocalDateTime publishedAt,
    String gender,
    Integer ageYears,
    String height,
    String weight,
    String classTeacherName,
    Integer daysSchoolOpened,
    Integer daysPresent,
    String reportDate,
    String classTeacherComment,
    String headTeacherRemark,
    List<SubjectComment> subjects
) {
    public record SubjectComment(String name, String comment) {
    }
}
