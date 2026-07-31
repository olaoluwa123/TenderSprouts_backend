package com.example.backend.dto.classroom;

import com.example.backend.entity.ClassSubject;

public record ClassSubjectResponse(Long id, Long classId, Long subjectId, Boolean isActive) {
    public static ClassSubjectResponse from(ClassSubject cs) {
        return new ClassSubjectResponse(cs.getId(), cs.getClassId(), cs.getSubjectId(), cs.getIsActive());
    }
}
