package com.example.backend.dto.student;

import java.util.List;

public record BulkPromoteResponse(
    int promoted,
    int skipped,
    List<BulkPromoteResultItem> results
) {
}
