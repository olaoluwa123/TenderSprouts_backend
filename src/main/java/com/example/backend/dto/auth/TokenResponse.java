package com.example.backend.dto.auth;

import com.example.backend.entity.User;

public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresInMs,
    Long userId,
    String role,
    Long profileId,
    boolean passwordChangeRequired
) {
    public static TokenResponse of(
        String accessToken,
        String refreshToken,
        long expiresInMs,
        Long userId,
        User.Role role,
        Long profileId,
        boolean passwordChangeRequired) {
        return new TokenResponse(
            accessToken,
            refreshToken,
            "Bearer",
            expiresInMs,
            userId,
            role.name(),
            profileId,
            passwordChangeRequired);
    }
}
