package com.example.backend.dto.teacher;

public record TeacherImportCsvRowResult(
    int rowNumber,
    String status,
    String message,
    Long teacherId
) {
}
