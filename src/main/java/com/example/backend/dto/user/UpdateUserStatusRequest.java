package com.example.backend.dto.user;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
    @NotNull Boolean isActive
) {
}
