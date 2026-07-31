package com.example.backend.controller;

import com.example.backend.dto.user.ForcePasswordResetResponse;
import com.example.backend.dto.user.UpdateUserStatusRequest;
import com.example.backend.dto.user.UserResponse;
import com.example.backend.entity.User;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserManagementService userManagementService;

    public UserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public Page<UserResponse> list(
        @RequestParam(required = false) User.Role role,
        @RequestParam(required = false) Boolean isActive,
        Pageable pageable) {
        return userManagementService.list(role, isActive, pageable);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userManagementService.getById(id);
    }

    @PatchMapping("/{id}/status")
    public UserResponse updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateUserStatusRequest request) {
        return userManagementService.updateStatus(id, request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/{id}/force-password-reset")
    public ForcePasswordResetResponse forcePasswordReset(@PathVariable Long id) {
        return userManagementService.forcePasswordReset(id, SecurityUtils.requireCurrentUser());
    }
}
