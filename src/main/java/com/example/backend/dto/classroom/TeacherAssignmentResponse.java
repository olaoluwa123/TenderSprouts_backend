package com.example.backend.dto.classroom;

public record TeacherAssignmentResponse(
    Long id,
    Long teacherId,
    Long classId,
    String className,
    Boolean isActive
) {
}
