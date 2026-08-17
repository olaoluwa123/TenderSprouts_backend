package com.example.backend.dto.attendance;

import com.example.backend.entity.enums.AttendanceStatus;
import java.time.LocalDate;

public record PupilAttendanceResponse(
    Long id,
    Long studentId,
    String studentName,
    String admissionNumber,
    Long classId,
    String className,
    LocalDate date,
    AttendanceStatus status,
    Long markedBy
) {
}
