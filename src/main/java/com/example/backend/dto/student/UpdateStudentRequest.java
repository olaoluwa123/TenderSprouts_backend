package com.example.backend.dto.student;

import com.example.backend.entity.enums.Gender;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record UpdateStudentRequest(
    String firstName,
    String lastName,
    LocalDate dateOfBirth,
    Gender gender,
    Boolean isActive,
    String address,
    String medicalInformation,
    @JsonProperty("height") String height,
    @JsonProperty("weight") String weight
) {
}
