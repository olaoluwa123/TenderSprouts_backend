package com.example.backend.dto.announcement;

import com.example.backend.entity.enums.AnnouncementAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record CreateAnnouncementRequest(
    @NotNull AnnouncementAudience audience,
    @NotBlank String title,
    @NotBlank String content,
    Boolean isImportant,
    Long classId,
    List<Long> recipientIds,
    LocalDateTime publishedAt,
    LocalDateTime expiresAt
) {
}
