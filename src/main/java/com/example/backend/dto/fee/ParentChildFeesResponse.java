package com.example.backend.dto.fee;

import java.math.BigDecimal;
import java.util.List;

public record ParentChildFeesResponse(
    Long studentId,
    String studentName,
    String admissionNumber,
    Long classId,
    String className,
    BigDecimal totalAmount,
    BigDecimal outstandingAmount,
    List<InvoiceResponse> invoices,
    boolean canGenerateFees,
    int missingInvoiceCount
) {
}
