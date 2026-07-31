package com.example.backend.dto.examtimetable;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateExamTimetableSlotRequest(
    Long subjectId,
    Long teacherId,
    LocalDate examDate,
    LocalTime startTime,
    LocalTime endTime,
    String room,
    String instructions
) {
}
