package com.example.backend.dto.attendance;

import com.example.backend.entity.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record StaffAttendanceMarkItem(
    @NotNull Long teacherId,
    @NotNull AttendanceStatus status
) {
}
