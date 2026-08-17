package com.example.backend.dto.report;

import java.util.List;

public record ReportDataResponse(
    String title,
    List<String> columns,
    List<List<Object>> rows
) {
}
