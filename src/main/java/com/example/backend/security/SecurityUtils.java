package com.example.backend.security;

import com.example.backend.entity.User;
import com.example.backend.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static SecurityUser requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUser user)) {
            throw new UnauthorizedException("Not authenticated");
        }
        return user;
    }

    public static boolean hasRole(User.Role role) {
        return requireCurrentUser().getRole() == role;
    }
}
