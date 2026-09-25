package com.example.backend.dto.classroom;

import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.enums.ClassGroup;

public record SchoolClassResponse(Long id, String name, ClassGroup classGroup, Boolean isActive) {
    public static SchoolClassResponse from(SchoolClass schoolClass) {
        return new SchoolClassResponse(
            schoolClass.getId(),
            schoolClass.getName(),
            schoolClass.getClassGroup(),
            schoolClass.getIsActive());
    }
}
