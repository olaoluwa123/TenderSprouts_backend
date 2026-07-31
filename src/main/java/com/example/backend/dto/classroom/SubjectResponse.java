package com.example.backend.dto.classroom;

import com.example.backend.entity.Subject;

public record SubjectResponse(Long id, String name, Boolean isActive) {
    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(subject.getId(), subject.getName(), subject.getIsActive());
    }
}
