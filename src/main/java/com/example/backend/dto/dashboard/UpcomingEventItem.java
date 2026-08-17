package com.example.backend.dto.dashboard;

import java.time.LocalDate;

public record UpcomingEventItem(
    Long id,
    String title,
    String eventType,
    LocalDate startDate,
    LocalDate endDate,
    String className,
    String description
) {
}
