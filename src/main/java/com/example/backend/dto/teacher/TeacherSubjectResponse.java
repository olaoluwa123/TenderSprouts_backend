package com.example.backend.dto.teacher;

public record TeacherSubjectResponse(
    Long id,
    Long teacherId,
    Long subjectId,
    String subjectName,
    Long classId,
    String className,
    Boolean isActive
) {
}
