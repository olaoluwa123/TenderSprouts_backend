package com.example.backend.dto.dashboard;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpcomingExamItem(
    Long id,
    String subjectName,
    LocalDate examDate,
    LocalTime startTime,
    LocalTime endTime,
    String room
) {
}
