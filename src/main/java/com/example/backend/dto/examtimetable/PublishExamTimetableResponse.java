package com.example.backend.dto.examtimetable;

public record PublishExamTimetableResponse(
    int slotsPublished,
    int parentsNotified
) {
}
