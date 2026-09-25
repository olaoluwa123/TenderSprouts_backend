package com.example.backend.dto.teacher;

import com.example.backend.entity.Teacher;
import java.util.List;

public record TeacherResponse(
    Long id,
    Long userId,
    String email,
    String fullName,
    String phone,
    Boolean isActive,
    Boolean loginActive,
    Long classId,
    String className,
    List<ClassSummary> classes
) {
    public record ClassSummary(Long classId, String className) {
    }

    public static TeacherResponse from(Teacher teacher, String email) {
        return from(teacher, email, null, null, null, List.of());
    }

    public static TeacherResponse from(
        Teacher teacher,
        String email,
        Boolean loginActive,
        Long classId,
        String className) {
        return from(teacher, email, loginActive, classId, className, List.of());
    }

    public static TeacherResponse from(
        Teacher teacher,
        String email,
        Boolean loginActive,
        Long classId,
        String className,
        List<ClassSummary> classes) {
        return new TeacherResponse(
            teacher.getId(),
            teacher.getUserId(),
            email,
            teacher.getFullName(),
            teacher.getPhone(),
            teacher.getIsActive(),
            loginActive,
            classId,
            className,
            classes == null ? List.of() : classes);
    }
}
