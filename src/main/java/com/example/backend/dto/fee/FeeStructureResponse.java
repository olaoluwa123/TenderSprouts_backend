package com.example.backend.dto.fee;

import com.example.backend.entity.FeeStructure;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FeeStructureResponse(
    Long id,
    String name,
    Long classId,
    String className,
    Long sessionId,
    Long termId,
    String termName,
    BigDecimal amount,
    String description,
    LocalDate dueDate,
    LocalDateTime publishedAt,
    Boolean isActive,
    long invoiceCount
) {
    public static FeeStructureResponse from(
        FeeStructure fee,
        String className,
        String termName,
        long invoiceCount) {
        return new FeeStructureResponse(
            fee.getId(),
            fee.getName(),
            fee.getClassId(),
            className,
            fee.getSessionId(),
            fee.getTermId(),
            termName,
            fee.getAmount(),
            fee.getDescription(),
            fee.getDueDate(),
            fee.getPublishedAt(),
            Boolean.TRUE.equals(fee.getIsActive()),
            invoiceCount);
    }
}
