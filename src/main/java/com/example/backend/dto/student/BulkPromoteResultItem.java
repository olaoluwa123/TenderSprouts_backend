package com.example.backend.dto.student;

public record BulkPromoteResultItem(
    Long studentId,
    String status,
    String message
) {
}
