package com.example.backend.dto.classroom;

import com.example.backend.entity.ClassSubject;

public record ClassSubjectResponse(
    Long id,
    Long classId,
    Long subjectId,
    String subjectName,
    Boolean isActive
) {
    public static ClassSubjectResponse from(ClassSubject cs) {
        return from(cs, null);
    }

    public static ClassSubjectResponse from(ClassSubject cs, String subjectName) {
        return new ClassSubjectResponse(
            cs.getId(),
            cs.getClassId(),
            cs.getSubjectId(),
            subjectName,
            cs.getIsActive());
    }
}
