package com.example.backend.dto.teacher;

import com.example.backend.entity.Teacher;

public record TeacherResponse(
    Long id,
    Long userId,
    String email,
    String fullName,
    String phone,
    Boolean isActive
) {
    public static TeacherResponse from(Teacher teacher, String email) {
        return new TeacherResponse(
            teacher.getId(),
            teacher.getUserId(),
            email,
            teacher.getFullName(),
            teacher.getPhone(),
            teacher.getIsActive());
    }
}
