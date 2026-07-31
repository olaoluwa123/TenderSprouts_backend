package com.example.backend.dto.classroom;

import com.example.backend.entity.SchoolClass;

public record SchoolClassResponse(Long id, String name, Boolean isActive) {
    public static SchoolClassResponse from(SchoolClass schoolClass) {
        return new SchoolClassResponse(schoolClass.getId(), schoolClass.getName(), schoolClass.getIsActive());
    }
}
