package com.example.backend.dto.teacher;

import com.example.backend.entity.Teacher;

public record TeacherResponse(
    Long id,
    Long userId,
    String email,
    String fullName,
    String phone,
    Boolean isActive,
    Boolean loginActive,
    Long classId,
    String className
) {
    public static TeacherResponse from(Teacher teacher, String email) {
        return from(teacher, email, null, null, null);
    }

    public static TeacherResponse from(
        Teacher teacher,
        String email,
        Boolean loginActive,
        Long classId,
        String className) {
        return new TeacherResponse(
            teacher.getId(),
            teacher.getUserId(),
            email,
            teacher.getFullName(),
            teacher.getPhone(),
            teacher.getIsActive(),
            loginActive,
            classId,
            className);
    }
}
