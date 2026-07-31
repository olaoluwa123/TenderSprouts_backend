package com.example.backend.dto.session;

import com.example.backend.entity.AcademicSession;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AcademicSessionResponse(
    Long id,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    Boolean isActive,
    LocalDateTime createdAt
) {
    public static AcademicSessionResponse from(AcademicSession session) {
        return new AcademicSessionResponse(
            session.getId(),
            session.getName(),
            session.getStartDate(),
            session.getEndDate(),
            session.getIsActive(),
            session.getCreatedAt());
    }
}
