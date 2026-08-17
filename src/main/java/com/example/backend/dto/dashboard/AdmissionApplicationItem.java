package com.example.backend.dto.dashboard;

import java.time.LocalDateTime;

public record AdmissionApplicationItem(
    Long id,
    String applicantName,
    String parentName,
    String parentEmail,
    String prospectiveClassName,
    String status,
    LocalDateTime createdAt
) {
}
