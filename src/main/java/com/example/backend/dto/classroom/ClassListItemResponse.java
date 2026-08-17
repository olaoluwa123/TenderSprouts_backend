package com.example.backend.dto.classroom;

public record ClassListItemResponse(
    Long id,
    String name,
    Boolean isActive,
    Long teacherId,
    String teacherName,
    long pupilCount,
    long subjectCount
) {
}
