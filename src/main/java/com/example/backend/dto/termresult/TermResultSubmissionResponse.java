package com.example.backend.dto.termresult;

import com.example.backend.entity.TermResultSubmission;
import com.example.backend.entity.enums.TermResultSubmissionStatus;
import java.time.LocalDateTime;

public record TermResultSubmissionResponse(
    Long id,
    Long classId,
    Long termId,
    Long sessionId,
    TermResultSubmissionStatus status,
    LocalDateTime submittedAt,
    Long submittedByTeacherId,
    LocalDateTime publishedAt
) {
    public static TermResultSubmissionResponse from(TermResultSubmission submission) {
        return new TermResultSubmissionResponse(
            submission.getId(),
            submission.getClassId(),
            submission.getTermId(),
            submission.getSessionId(),
            submission.getStatus(),
            submission.getSubmittedAt(),
            submission.getSubmittedByTeacherId(),
            submission.getPublishedAt());
    }
}
