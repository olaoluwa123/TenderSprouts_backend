package com.example.backend.dto.teacher;

import com.example.backend.entity.TeacherTimetableSlot;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record TeacherTimetableSlotResponse(
    Long id,
    Long teacherId,
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    Long classId,
    String className,
    Long subjectId,
    String subjectName,
    String room
) {
    public static TeacherTimetableSlotResponse from(
        TeacherTimetableSlot slot,
        String className,
        String subjectName) {
        return new TeacherTimetableSlotResponse(
            slot.getId(),
            slot.getTeacherId(),
            slot.getDayOfWeek(),
            slot.getStartTime(),
            slot.getEndTime(),
            slot.getClassId(),
            className,
            slot.getSubjectId(),
            subjectName,
            slot.getRoom());
    }
}
