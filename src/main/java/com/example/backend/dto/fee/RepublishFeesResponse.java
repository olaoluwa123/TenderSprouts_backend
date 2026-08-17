package com.example.backend.dto.fee;

public record RepublishFeesResponse(
    Long classId,
    Long termId,
    int pupilsInvoiced,
    int invoicesCreated
) {
}
