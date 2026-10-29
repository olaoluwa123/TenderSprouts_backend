package com.example.backend.dto.assessment;

import java.time.LocalDateTime;

public record MidtermSubmissionSummaryResponse(
    Long id,
    Long classId,
    String className,
    Long termId,
    String termName,
    Long sessionId,
    String status,
    LocalDateTime submittedAt,
    Long submittedByTeacherId,
    String submittedByTeacherName,
    int pupilCount
) {
}
