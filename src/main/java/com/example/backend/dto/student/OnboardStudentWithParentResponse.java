package com.example.backend.dto.student;

import com.example.backend.dto.parent.ParentResponse;

public record OnboardStudentWithParentResponse(
    StudentResponse student,
    ParentResponse parent
) {
}
