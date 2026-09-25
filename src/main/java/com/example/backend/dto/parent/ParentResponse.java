package com.example.backend.dto.parent;

import com.example.backend.entity.Parent;

public record ParentResponse(
    Long id,
    Long userId,
    String email,
    String fullName,
    String phone,
    String address,
    Boolean isActive
) {
    public static ParentResponse from(Parent parent, String email) {
        return new ParentResponse(
            parent.getId(),
            parent.getUserId(),
            email,
            parent.getFullName(),
            parent.getPhone(),
            parent.getAddress(),
            parent.getIsActive());
    }
}
