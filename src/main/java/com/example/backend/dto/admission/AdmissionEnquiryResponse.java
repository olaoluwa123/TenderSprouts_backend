package com.example.backend.dto.admission;

import java.time.LocalDateTime;

public record AdmissionEnquiryResponse(
    Long id,
    String enquirerName,
    String email,
    String phone,
    String childAgeOrClass,
    String message,
    String status,
    LocalDateTime createdAt
) {
}
