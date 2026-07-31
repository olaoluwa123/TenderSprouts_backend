package com.example.backend.dto.student;

import java.util.List;

public record ImportCsvResponse(
    int totalRows,
    int successCount,
    int failureCount,
    List<ImportCsvRowResult> rows
) {
}
