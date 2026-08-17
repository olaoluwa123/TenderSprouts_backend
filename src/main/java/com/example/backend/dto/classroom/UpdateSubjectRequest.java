package com.example.backend.dto.classroom;

public record UpdateSubjectRequest(
    String name,
    Boolean isActive
) {
}
