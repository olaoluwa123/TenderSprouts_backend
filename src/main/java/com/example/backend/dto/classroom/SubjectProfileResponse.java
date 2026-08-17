package com.example.backend.dto.classroom;

import java.util.List;

public record SubjectProfileResponse(
    Long id,
    String name,
    Boolean isActive,
    List<ClassAssignment> classes,
    List<TeacherAssignment> teachers
) {
    public record ClassAssignment(Long classId, String className, Boolean isActive) {
    }

    public record TeacherAssignment(
        Long teacherId,
        String teacherName,
        Long classId,
        String className
    ) {
    }
}
