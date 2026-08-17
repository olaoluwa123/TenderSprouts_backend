package com.example.backend.dto.teacher;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record CreateTeacherTimetableSlotRequest(
    @NotNull DayOfWeek dayOfWeek,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    @NotNull Long classId,
    Long subjectId,
    String room
) {
}
