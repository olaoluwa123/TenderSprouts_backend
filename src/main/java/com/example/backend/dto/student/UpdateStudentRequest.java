package com.example.backend.dto.student;

import com.example.backend.entity.enums.Gender;
import java.time.LocalDate;

public record UpdateStudentRequest(
    String firstName,
    String lastName,
    LocalDate dateOfBirth,
    Gender gender,
    Boolean isActive,
    String address,
    String medicalInformation
) {
}
