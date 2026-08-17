package com.example.backend.dto.announcement;

import com.example.backend.entity.enums.AnnouncementRecipientType;

public record AnnouncementRecipientResponse(
    Long id,
    AnnouncementRecipientType recipientType,
    Long recipientId
) {
}
