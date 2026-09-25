package com.example.backend.dto.preschool;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PreschoolReportResponse(
    Long reportId,
    Long classId,
    Long termId,
    LocalDateTime publishedAt,
    List<Subject> subjects,
    List<Pupil> pupils
) {
    public record Subject(Long subjectId, String subjectName) {
    }

    public record Pupil(
        Long studentId,
        String studentName,
        String admissionNumber,
        Map<Long, String> comments,
        String classTeacherComment,
        String headTeacherRemark
    ) {
    }
}
