package com.example.backend.dto.teacher;

import java.util.List;

public record TeacherImportCsvJobResponse(
    Long jobId,
    String status,
    int totalRows,
    int successCount,
    int failureCount,
    List<TeacherImportCsvRowResult> rows,
    String errorMessage
) {
}
