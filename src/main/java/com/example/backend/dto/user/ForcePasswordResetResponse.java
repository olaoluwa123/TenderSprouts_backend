package com.example.backend.dto.user;

public record ForcePasswordResetResponse(
    Long userId,
    String email,
    boolean emailSent
) {
}
