package com.example.backend.dto.user;

import com.example.backend.entity.User;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
    @NotNull User.Role role
) {
}
