package com.example.backend.dto.termresult;

import com.example.backend.entity.TermResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TermResultResponse(
    Long id,
    Long studentId,
    Long termId,
    Long sessionId,
    Long classId,
    BigDecimal averageScore,
    Integer rankInClass,
    String comments,
    LocalDateTime publishedAt,
    LocalDateTime createdAt
) {
    public static TermResultResponse from(TermResult result) {
        return new TermResultResponse(
            result.getId(),
            result.getStudentId(),
            result.getTermId(),
            result.getSessionId(),
            result.getClassId(),
            result.getAverageScore(),
            result.getRankInClass(),
            result.getComments(),
            result.getPublishedAt(),
            result.getCreatedAt());
    }
}
