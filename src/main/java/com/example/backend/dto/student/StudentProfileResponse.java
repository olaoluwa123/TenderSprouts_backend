package com.example.backend.dto.student;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StudentProfileResponse(
    Long id,
    String firstName,
    String lastName,
    LocalDate dateOfBirth,
    String gender,
    String admissionNumber,
    Long classId,
    String className,
    Long sessionId,
    String sessionName,
    List<GuardianItem> guardians,
    String address,
    String medicalInformation,
    Boolean isActive,
    List<ResultItem> results,
    AttendanceOverview attendance,
    List<SubjectItem> subjects,
    List<ClassHistoryItem> classHistory
) {
    public record GuardianItem(
        Long id,
        String fullName,
        String email,
        String phone
    ) {
    }

    public record ResultItem(
        Long termResultId,
        Long termId,
        String termName,
        String sessionName,
        String className,
        BigDecimal averageScore,
        String letterGrade,
        Integer rankInClass,
        boolean published
    ) {
    }

    public record AttendanceOverview(
        long present,
        long absent,
        long late,
        long excused,
        long totalMarked
    ) {
    }

    public record SubjectItem(
        Long id,
        String name
    ) {
    }

    public record ClassHistoryItem(
        Long enrollmentId,
        Long classId,
        String className,
        Long sessionId,
        String sessionName,
        LocalDate startDate,
        LocalDate endDate,
        String status
    ) {
    }
}
