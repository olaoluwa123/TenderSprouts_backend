package com.example.backend.dto.student;

import java.util.List;

public record ImportCsvJobAcceptedResponse(
    Long jobId,
    String status
) {
}
