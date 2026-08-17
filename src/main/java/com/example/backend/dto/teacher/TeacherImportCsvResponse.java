package com.example.backend.dto.teacher;

import java.util.List;

public record TeacherImportCsvResponse(
    int totalRows,
    int successCount,
    int failureCount,
    List<TeacherImportCsvRowResult> rows
) {
}
