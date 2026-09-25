package com.example.backend.dto.classroom;

import com.example.backend.entity.enums.ClassGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateSchoolClassRequest(
    @NotBlank String name,
    @NotNull ClassGroup classGroup,
    @NotNull Boolean isActive
) {
}
