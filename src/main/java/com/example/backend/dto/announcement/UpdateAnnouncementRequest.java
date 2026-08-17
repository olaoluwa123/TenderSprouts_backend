package com.example.backend.dto.announcement;

import com.example.backend.entity.enums.AnnouncementAudience;
import java.time.LocalDateTime;
import java.util.List;

public record UpdateAnnouncementRequest(
    AnnouncementAudience audience,
    String title,
    String content,
    Boolean isImportant,
    Long classId,
    List<Long> recipientIds,
    LocalDateTime publishedAt,
    LocalDateTime expiresAt
) {
}
