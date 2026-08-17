package com.example.backend.dto.announcement;

import com.example.backend.entity.Announcement;
import com.example.backend.entity.enums.AnnouncementAudience;
import java.time.LocalDateTime;
import java.util.List;

public record AnnouncementResponse(
    Long id,
    String title,
    String content,
    AnnouncementAudience audience,
    Boolean isImportant,
    Long classId,
    String className,
    Long createdBy,
    LocalDateTime publishedAt,
    LocalDateTime expiresAt,
    LocalDateTime createdAt,
    List<AnnouncementRecipientResponse> recipients
) {
    public static AnnouncementResponse from(
        Announcement announcement,
        String className,
        List<AnnouncementRecipientResponse> recipients) {
        return new AnnouncementResponse(
            announcement.getId(),
            announcement.getTitle(),
            announcement.getContent(),
            announcement.getAudience(),
            Boolean.TRUE.equals(announcement.getIsImportant()),
            announcement.getClassId(),
            className,
            announcement.getCreatedBy(),
            announcement.getPublishedAt(),
            announcement.getExpiresAt(),
            announcement.getCreatedAt(),
            recipients);
    }
}
