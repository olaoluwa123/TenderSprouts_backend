package com.example.backend.dto.student;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateEnrollmentRequest(
    @NotNull Long classId,
    Long sessionId,
    LocalDate startDate
) {
}
