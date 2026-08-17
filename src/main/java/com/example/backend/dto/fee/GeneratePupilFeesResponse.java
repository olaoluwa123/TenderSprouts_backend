package com.example.backend.dto.fee;

public record GeneratePupilFeesResponse(
    Long studentId,
    Long classId,
    Long termId,
    int invoicesCreated
) {
}
