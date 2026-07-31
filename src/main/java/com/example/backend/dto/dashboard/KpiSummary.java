package com.example.backend.dto.dashboard;

public record KpiSummary(
    long activeStudents,
    long activeTeachers,
    long activeParents,
    long classes
) {
}
