package com.example.backend.dto.classroom;

public record BulkAssignClassSubjectsResponse(
    int assignedCount,
    int skippedCount
) {
}
