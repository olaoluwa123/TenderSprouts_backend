package com.example.backend.dto.session;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreateSessionRequest(
    @NotBlank String name,
    LocalDate startDate,
    LocalDate endDate
) {
}
