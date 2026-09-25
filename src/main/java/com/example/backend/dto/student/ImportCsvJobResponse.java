package com.example.backend.dto.student;

import java.util.List;

public record ImportCsvJobResponse(
    Long jobId,
    String status,
    int totalRows,
    int successCount,
    int failureCount,
    List<ImportCsvRowResult> rows,
    String errorMessage
) {
}
