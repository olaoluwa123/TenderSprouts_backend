package com.example.backend.dto.calendar;

import java.time.LocalDateTime;

public record CalendarPosterResponse(
    boolean exists,
    String originalFilename,
    String contentType,
    LocalDateTime uploadedAt
) {
    public static CalendarPosterResponse missing() {
        return new CalendarPosterResponse(false, null, null, null);
    }

    public static CalendarPosterResponse present(String originalFilename, String contentType, LocalDateTime uploadedAt) {
        return new CalendarPosterResponse(true, originalFilename, contentType, uploadedAt);
    }
}
