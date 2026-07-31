package com.example.backend.dto.dashboard;

import java.time.LocalDateTime;

public record PendingApprovalItem(
    Long submissionId,
    Long classId,
    String className,
    Long termId,
    String termName,
    String submittedByTeacherName,
    int resultCount,
    LocalDateTime submittedAt
) {
}
