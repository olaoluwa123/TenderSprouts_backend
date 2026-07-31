package com.example.backend.dto.session;

import com.example.backend.entity.Term;
import java.time.LocalDate;

public record TermResponse(
    Long id,
    Long sessionId,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    Boolean isActive
) {
    public static TermResponse from(Term term) {
        return new TermResponse(
            term.getId(),
            term.getSessionId(),
            term.getName(),
            term.getStartDate(),
            term.getEndDate(),
            term.getIsActive());
    }
}
