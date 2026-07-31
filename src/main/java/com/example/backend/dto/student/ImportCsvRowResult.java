package com.example.backend.dto.student;

public record ImportCsvRowResult(
    int rowNumber,
    String status,
    String message,
    Long studentId,
    Long parentId
) {
}
