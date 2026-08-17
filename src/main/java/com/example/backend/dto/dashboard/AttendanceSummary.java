package com.example.backend.dto.dashboard;

import java.time.LocalDate;

public record AttendanceSummary(
    LocalDate date,
    long present,
    long absent,
    long late,
    long excused,
    long marked,
    long enrolled
) {
}
