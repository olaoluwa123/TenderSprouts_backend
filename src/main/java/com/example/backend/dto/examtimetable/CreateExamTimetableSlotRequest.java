package com.example.backend.dto.examtimetable;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateExamTimetableSlotRequest(
    @NotNull Long subjectId,
    Long teacherId,
    @NotNull Long termId,
    Long sessionId,
    @NotNull LocalDate examDate,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    String room,
    String instructions
) {
}
