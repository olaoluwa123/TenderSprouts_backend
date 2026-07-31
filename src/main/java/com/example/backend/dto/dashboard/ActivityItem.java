package com.example.backend.dto.dashboard;

import java.time.LocalDateTime;

public record ActivityItem(
    Long id,
    String action,
    String entityType,
    Long entityId,
    String details,
    String userRole,
    LocalDateTime createdAt
) {
}
