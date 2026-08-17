package com.example.backend.dto.fee;

public record RepublishPupilPreviewItem(
    Long studentId,
    String studentName,
    String admissionNumber,
    int missingInvoiceCount
) {
}
