package com.example.backend.dto.classroom;

import com.example.backend.entity.enums.ClassGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSchoolClassRequest(
    @NotBlank String name,
    @NotNull ClassGroup classGroup
) {
}
