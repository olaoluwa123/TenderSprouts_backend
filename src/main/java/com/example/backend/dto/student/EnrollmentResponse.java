package com.example.backend.dto.student;

import com.example.backend.entity.StudentEnrollment;
import com.example.backend.entity.enums.EnrollmentStatus;
import java.time.LocalDate;

public record EnrollmentResponse(
    Long id,
    Long studentId,
    Long classId,
    Long sessionId,
    LocalDate startDate,
    LocalDate endDate,
    EnrollmentStatus status
) {
    public static EnrollmentResponse from(StudentEnrollment enrollment) {
        return new EnrollmentResponse(
            enrollment.getId(),
            enrollment.getStudentId(),
            enrollment.getClassId(),
            enrollment.getSessionId(),
            enrollment.getStartDate(),
            enrollment.getEndDate(),
            enrollment.getStatus());
    }
}
