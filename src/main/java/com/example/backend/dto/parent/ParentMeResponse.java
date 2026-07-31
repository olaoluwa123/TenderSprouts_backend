package com.example.backend.dto.parent;

import com.example.backend.dto.student.StudentResponse;
import java.util.List;

public record ParentMeResponse(
    Long parentId,
    Long userId,
    String email,
    String fullName,
    String phone,
    List<StudentResponse> children
) {
}
