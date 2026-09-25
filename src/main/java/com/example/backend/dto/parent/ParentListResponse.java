package com.example.backend.dto.parent;

import com.example.backend.entity.Parent;
import java.util.List;

public record ParentListResponse(
    Long id,
    Long userId,
    String email,
    String fullName,
    String phone,
    String address,
    Boolean isActive,
    Boolean loginActive,
    List<LinkedStudentSummary> students
) {
    public static ParentListResponse from(
        Parent parent,
        String email,
        Boolean loginActive,
        List<LinkedStudentSummary> students) {
        return new ParentListResponse(
            parent.getId(),
            parent.getUserId(),
            email,
            parent.getFullName(),
            parent.getPhone(),
            parent.getAddress(),
            parent.getIsActive(),
            loginActive,
            students);
    }
}
