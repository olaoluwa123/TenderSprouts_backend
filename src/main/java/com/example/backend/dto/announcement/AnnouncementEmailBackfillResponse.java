package com.example.backend.dto.announcement;

public record AnnouncementEmailBackfillResponse(
    int total,
    int notified,
    int skippedExpired
) {
}
