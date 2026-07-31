package com.example.backend.dto.parent;

import com.example.backend.entity.Student;

public record LinkedStudentSummary(
    Long id,
    String firstName,
    String lastName,
    String admissionNumber,
    Long classId
) {
    public static LinkedStudentSummary from(Student student) {
        return new LinkedStudentSummary(
            student.getId(),
            student.getFirstName(),
            student.getLastName(),
            student.getAdmissionNumber(),
            student.getClassId());
    }
}
