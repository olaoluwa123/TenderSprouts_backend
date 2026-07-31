package com.example.backend.dto.user;

import com.example.backend.entity.User;
import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String email,
    String fullName,
    User.Role role,
    boolean isActive,
    boolean mustChangePassword,
    Long profileId,
    LocalDateTime lastLoginAt
) {
}
