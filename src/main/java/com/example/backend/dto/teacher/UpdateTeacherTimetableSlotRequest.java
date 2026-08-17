package com.example.backend.dto.teacher;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record UpdateTeacherTimetableSlotRequest(
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    Long classId,
    Long subjectId,
    String room
) {
}
