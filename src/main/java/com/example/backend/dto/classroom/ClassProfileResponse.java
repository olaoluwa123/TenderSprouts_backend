package com.example.backend.dto.classroom;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ClassProfileResponse(
    Long id,
    String name,
    Boolean isActive,
    TeacherInfo teacher,
    long pupilCount,
    List<SubjectInfo> subjects,
    List<PupilInfo> pupils,
    AttendanceInfo attendance,
    ResultsInfo results
) {
    public record TeacherInfo(Long id, String fullName, String phone) {
    }

    public record SubjectInfo(
        Long subjectId,
        String subjectName,
        Boolean isActive,
        List<String> teacherNames
    ) {
    }

    public record PupilInfo(
        Long id,
        String firstName,
        String lastName,
        String admissionNumber,
        Boolean isActive
    ) {
    }

    public record AttendanceInfo(
        LocalDate date,
        long present,
        long absent,
        long late,
        long excused,
        long marked,
        long enrolled,
        List<AttendancePupil> pupils
    ) {
    }

    public record AttendancePupil(
        Long studentId,
        String fullName,
        String admissionNumber,
        String status
    ) {
    }

    public record ResultsInfo(
        Long termId,
        String termName,
        List<ResultItem> items
    ) {
    }

    public record ResultItem(
        Long studentId,
        String studentName,
        String admissionNumber,
        BigDecimal averageScore,
        Integer rankInClass,
        LocalDateTime publishedAt
    ) {
    }
}
