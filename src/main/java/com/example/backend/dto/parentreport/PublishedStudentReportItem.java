package com.example.backend.dto.parentreport;

import java.time.LocalDateTime;

public record PublishedStudentReportItem(
    String kind,
    Long termId,
    String termName,
    Long sessionId,
    String sessionName,
    String className,
    Integer weekNumber,
    String periodType,
    LocalDateTime publishedAt,
    Integer monthNumber
) {
}
