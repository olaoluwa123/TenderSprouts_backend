package com.example.backend.dto.student;

import com.example.backend.entity.Student;
import com.example.backend.entity.enums.Gender;
import java.time.LocalDate;

public record StudentResponse(
    Long id,
    String firstName,
    String lastName,
    LocalDate dateOfBirth,
    String admissionNumber,
    Gender gender,
    Long classId,
    Long currentEnrollmentId,
    Boolean isActive
) {
    public static StudentResponse from(Student student) {
        return new StudentResponse(
            student.getId(),
            student.getFirstName(),
            student.getLastName(),
            student.getDateOfBirth(),
            student.getAdmissionNumber(),
            student.getGender(),
            student.getClassId(),
            student.getCurrentEnrollmentId(),
            student.getIsActive());
    }
}
