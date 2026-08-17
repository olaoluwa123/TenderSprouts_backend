package com.example.backend.dto.calendar;

import com.example.backend.entity.SchoolCalendarEvent;
import com.example.backend.entity.enums.CalendarEventType;
import java.time.LocalDate;

public record CalendarEventResponse(
    Long id,
    String title,
    CalendarEventType eventType,
    LocalDate startDate,
    LocalDate endDate,
    Long classId,
    String className,
    String description
) {
    public static CalendarEventResponse from(SchoolCalendarEvent event, String className) {
        return new CalendarEventResponse(
            event.getId(),
            event.getTitle(),
            event.getEventType(),
            event.getStartDate(),
            event.getEndDate(),
            event.getClassId(),
            className,
            event.getDescription());
    }
}
