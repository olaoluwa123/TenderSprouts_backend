package com.example.backend.dto.dashboard;

import java.time.LocalDateTime;

public record RecentPupilItem(
    Long id,
    String fullName,
    String admissionNumber,
    String className,
    LocalDateTime registeredAt
) {
}
