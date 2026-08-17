package com.example.backend.dto.teacher;

public record UpdateTeacherRequest(
    String fullName,
    String phone,
    Boolean isActive
) {
}
