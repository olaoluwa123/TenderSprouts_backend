package com.example.backend.dto.attendance;

import com.example.backend.entity.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record AttendanceMarkItem(
    @NotNull Long studentId,
    @NotNull AttendanceStatus status
) {
}
