package com.example.backend.dto.classroom;

import com.example.backend.entity.enums.ClassGroup;
import java.util.List;

public record ClassListItemResponse(
    Long id,
    String name,
    ClassGroup classGroup,
    Boolean isActive,
    Long teacherId,
    String teacherName,
    List<TeacherSummary> teachers,
    long pupilCount,
    long subjectCount
) {
    public record TeacherSummary(Long id, String fullName) {
    }
}
