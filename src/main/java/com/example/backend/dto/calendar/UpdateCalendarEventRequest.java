package com.example.backend.dto.calendar;

import com.example.backend.entity.enums.CalendarEventType;
import java.time.LocalDate;

public record UpdateCalendarEventRequest(
    String title,
    CalendarEventType eventType,
    LocalDate startDate,
    LocalDate endDate,
    Long classId,
    String description
) {
}
