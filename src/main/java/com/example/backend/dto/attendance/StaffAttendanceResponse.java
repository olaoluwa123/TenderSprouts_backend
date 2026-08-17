package com.example.backend.dto.attendance;

import com.example.backend.entity.enums.AttendanceStatus;
import java.time.LocalDate;

public record StaffAttendanceResponse(
    Long id,
    Long teacherId,
    String teacherName,
    LocalDate date,
    AttendanceStatus status,
    Long markedBy
) {
}
