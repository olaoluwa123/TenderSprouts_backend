package com.example.backend.dto.termresult;

import com.example.backend.entity.TermResultSubmission;
import com.example.backend.entity.enums.TermResultSubmissionStatus;
import java.time.LocalDateTime;

public record TermResultSubmissionSummaryResponse(
    Long id,
    Long classId,
    String className,
    Long termId,
    String termName,
    Long sessionId,
    TermResultSubmissionStatus status,
    LocalDateTime submittedAt,
    Long submittedByTeacherId,
    String submittedByTeacherName,
    int resultCount
) {
    public static TermResultSubmissionSummaryResponse from(
        TermResultSubmission submission,
        String className,
        String termName,
        String teacherName,
        int resultCount) {
        return new TermResultSubmissionSummaryResponse(
            submission.getId(),
            submission.getClassId(),
            className,
            submission.getTermId(),
            termName,
            submission.getSessionId(),
            submission.getStatus(),
            submission.getSubmittedAt(),
            submission.getSubmittedByTeacherId(),
            teacherName,
            resultCount);
    }
}
