package com.example.backend.dto.calendar;

import com.example.backend.entity.enums.CalendarEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateCalendarEventRequest(
    @NotBlank String title,
    @NotNull CalendarEventType eventType,
    @NotNull LocalDate startDate,
    LocalDate endDate,
    Long classId,
    String description
) {
}
