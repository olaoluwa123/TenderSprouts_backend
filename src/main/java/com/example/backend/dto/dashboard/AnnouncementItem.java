package com.example.backend.dto.dashboard;

import java.time.LocalDateTime;

public record AnnouncementItem(
    Long id,
    String title,
    String content,
    boolean important,
    String className,
    LocalDateTime createdAt
) {
}
