package com.example.backend.dto.fee;

import com.example.backend.entity.Invoice;
import com.example.backend.entity.enums.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvoiceResponse(
    Long id,
    Long studentId,
    String studentName,
    String admissionNumber,
    Long parentId,
    String parentName,
    String parentPhone,
    Long feeStructureId,
    String feeName,
    Long classId,
    String className,
    Long sessionId,
    String sessionName,
    Long termId,
    String termName,
    BigDecimal amount,
    LocalDate dueDate,
    InvoiceStatus status,
    LocalDateTime createdAt
) {
    public static InvoiceResponse from(
        Invoice invoice,
        String studentName,
        String admissionNumber,
        Long parentId,
        String parentName,
        String parentPhone,
        String feeName,
        Long classId,
        String className,
        Long sessionId,
        String sessionName,
        Long termId,
        String termName) {
        return new InvoiceResponse(
            invoice.getId(),
            invoice.getStudentId(),
            studentName,
            admissionNumber,
            parentId,
            parentName,
            parentPhone,
            invoice.getFeeStructureId(),
            feeName,
            classId,
            className,
            sessionId,
            sessionName,
            termId,
            termName,
            invoice.getAmount(),
            invoice.getDueDate(),
            invoice.getStatus(),
            invoice.getCreatedAt());
    }
}
