package com.example.backend.dto.teacher;

import java.time.LocalDate;
import java.util.List;

public record TeacherProfileResponse(
    Long id,
    Long userId,
    String email,
    String fullName,
    String phone,
    Boolean isActive,
    Boolean loginActive,
    List<ClassItem> classes,
    List<SubjectItem> subjects
) {
    public record ClassItem(
        Long assignmentId,
        Long classId,
        String className,
        LocalDate startDate,
        Boolean isActive
    ) {
    }

    public record SubjectItem(
        Long id,
        Long subjectId,
        String subjectName,
        Long classId,
        String className,
        Boolean isActive
    ) {
    }
}
