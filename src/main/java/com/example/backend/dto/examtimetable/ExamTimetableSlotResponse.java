package com.example.backend.dto.examtimetable;

import com.example.backend.entity.ExamTimetableSlot;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ExamTimetableSlotResponse(
    Long id,
    Long classId,
    String className,
    Long subjectId,
    String subjectName,
    Long teacherId,
    String teacherName,
    Long termId,
    Long sessionId,
    LocalDate examDate,
    LocalTime startTime,
    LocalTime endTime,
    String room,
    String instructions,
    LocalDateTime publishedAt
) {
    public static ExamTimetableSlotResponse from(
        ExamTimetableSlot slot,
        String className,
        String subjectName,
        String teacherName) {
        return new ExamTimetableSlotResponse(
            slot.getId(),
            slot.getClassId(),
            className,
            slot.getSubjectId(),
            subjectName,
            slot.getTeacherId(),
            teacherName,
            slot.getTermId(),
            slot.getSessionId(),
            slot.getExamDate(),
            slot.getStartTime(),
            slot.getEndTime(),
            slot.getRoom(),
            slot.getInstructions(),
            slot.getPublishedAt());
    }
}
