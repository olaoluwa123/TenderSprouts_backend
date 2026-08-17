package com.example.backend.service;

import com.example.backend.dto.dashboard.ActivityItem;
import com.example.backend.dto.user.ForcePasswordResetResponse;
import com.example.backend.dto.user.UpdateUserRoleRequest;
import com.example.backend.dto.user.UpdateUserStatusRequest;
import com.example.backend.dto.user.UserResponse;
import com.example.backend.entity.AuditLog;
import com.example.backend.entity.User;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AdminRepository;
import com.example.backend.repository.AuditLogRepository;
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.SecurityUser;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final ParentRepository parentRepository;
    private final AdminRepository adminRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordResetService passwordResetService;
    private final AuditService auditService;

    public UserManagementService(
        UserRepository userRepository,
        TeacherRepository teacherRepository,
        ParentRepository parentRepository,
        AdminRepository adminRepository,
        AuditLogRepository auditLogRepository,
        PasswordResetService passwordResetService,
        AuditService auditService) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.parentRepository = parentRepository;
        this.adminRepository = adminRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordResetService = passwordResetService;
        this.auditService = auditService;
    }

    public Page<UserResponse> list(User.Role role, Boolean isActive, Pageable pageable) {
        Page<User> page;
        if (role != null && isActive != null) {
            page = userRepository.findByRoleAndIsActive(role, isActive, pageable);
        } else if (role != null) {
            page = userRepository.findByRole(role, pageable);
        } else if (isActive != null) {
            page = userRepository.findByIsActive(isActive, pageable);
        } else {
            page = userRepository.findAll(pageable);
        }
        return page.map(this::toResponse);
    }

    public UserResponse getById(Long id) {
        return toResponse(findUser(id));
    }

    @Transactional
    public UserResponse updateStatus(Long id, UpdateUserStatusRequest request, SecurityUser actor) {
        User user = findUser(id);
        if (actor.getUserId().equals(id) && !request.isActive()) {
            throw new ConflictException("You cannot deactivate your own account");
        }
        user.setIsActive(request.isActive());
        syncProfileActive(user, request.isActive());
        if (!request.isActive()) {
            user.setAccessTokenVersion(user.getAccessTokenVersion() + 1);
        }
        user = userRepository.save(user);
        auditService.logAdminAction(actor,
            request.isActive() ? "REACTIVATE" : "DEACTIVATE",
            "user",
            user.getId(),
            user.getEmail());
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateRole(Long id, UpdateUserRoleRequest request, SecurityUser actor) {
        if (actor.getUserId().equals(id)) {
            throw new ConflictException("You cannot change your own role");
        }
        User user = findUser(id);
        User.Role newRole = request.role();
        if (user.getRole() == newRole) {
            return toResponse(user);
        }
        requireProfileForRole(user.getId(), newRole);
        User.Role previous = user.getRole();
        user.setRole(newRole);
        user.setAccessTokenVersion(user.getAccessTokenVersion() + 1);
        user = userRepository.save(user);
        auditService.logAdminAction(
            actor,
            "UPDATE_ROLE",
            "user",
            user.getId(),
            previous.name() + "->" + newRole.name() + " (" + user.getEmail() + ")");
        return toResponse(user);
    }

    public List<ActivityItem> listActivity(Long userId) {
        findUser(userId);
        Map<Long, AuditLog> merged = new LinkedHashMap<>();
        for (AuditLog log : auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("user", userId)) {
            merged.put(log.getId(), log);
        }
        for (AuditLog log : auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            merged.putIfAbsent(log.getId(), log);
        }
        return merged.values().stream()
            .sorted(Comparator.comparing(AuditLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(this::toActivityItem)
            .toList();
    }

    @Transactional
    public ForcePasswordResetResponse forcePasswordReset(Long id, SecurityUser actor) {
        User user = findUser(id);
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ConflictException("Cannot reset password for deactivated user");
        }
        String rawToken = passwordResetService.issueResetToken(user, true);
        String recipientName = resolveProfile(user).fullName();
        if (recipientName == null || recipientName.isBlank()) {
            recipientName = user.getEmail();
        }
        passwordResetService.sendResetEmail(user, rawToken, recipientName);
        auditService.logAdminAction(actor, "FORCE_PASSWORD_RESET", "user", user.getId(), user.getEmail());
        return new ForcePasswordResetResponse(user.getId(), user.getEmail(), true);
    }

    private void requireProfileForRole(Long userId, User.Role role) {
        boolean hasProfile = switch (role) {
            case TEACHER -> teacherRepository.findByUserId(userId).isPresent();
            case PARENT -> parentRepository.findByUserId(userId).isPresent();
            case ADMIN -> adminRepository.findByUserId(userId).isPresent();
        };
        if (!hasProfile) {
            throw new ConflictException(
                "No matching " + role.name().toLowerCase() + " profile exists for this user");
        }
    }

    private void syncProfileActive(User user, boolean active) {
        switch (user.getRole()) {
            case TEACHER -> teacherRepository.findByUserId(user.getId()).ifPresent(teacher -> {
                teacher.setIsActive(active);
                teacherRepository.save(teacher);
            });
            case PARENT -> parentRepository.findByUserId(user.getId()).ifPresent(parent -> {
                parent.setIsActive(active);
                parentRepository.save(parent);
            });
            case ADMIN -> { /* admins table has no is_active */ }
        }
    }

    private UserResponse toResponse(User user) {
        ProfileInfo profile = resolveProfile(user);
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            profile.fullName(),
            user.getRole(),
            Boolean.TRUE.equals(user.getIsActive()),
            Boolean.TRUE.equals(user.getMustChangePassword()),
            profile.profileId(),
            user.getLastLoginAt());
    }

    private ProfileInfo resolveProfile(User user) {
        return switch (user.getRole()) {
            case TEACHER -> teacherRepository.findByUserId(user.getId())
                .map(teacher -> new ProfileInfo(teacher.getId(), blankToNull(teacher.getFullName())))
                .orElse(new ProfileInfo(null, null));
            case PARENT -> parentRepository.findByUserId(user.getId())
                .map(parent -> new ProfileInfo(parent.getId(), blankToNull(parent.getFullName())))
                .orElse(new ProfileInfo(null, null));
            case ADMIN -> adminRepository.findByUserId(user.getId())
                .map(admin -> new ProfileInfo(admin.getId(), blankToNull(admin.getFullName())))
                .orElse(new ProfileInfo(null, null));
        };
    }

    private ActivityItem toActivityItem(AuditLog log) {
        return new ActivityItem(
            log.getId(),
            log.getAction(),
            log.getEntityType(),
            log.getEntityId(),
            log.getDetails(),
            log.getUserRole(),
            log.getCreatedAt());
    }

    private static String blankToNull(String value) {
        return value != null && !value.isBlank() ? value : null;
    }

    private record ProfileInfo(Long profileId, String fullName) {
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
