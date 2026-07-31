package com.example.backend.service;

import com.example.backend.config.AppFrontendProperties;
import com.example.backend.entity.Admin;
import com.example.backend.entity.Parent;
import com.example.backend.entity.PasswordResetToken;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.User;
import com.example.backend.repository.AdminRepository;
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.PasswordResetTokenRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.TokenHashUtil;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AdminRepository adminRepository;
    private final ParentRepository parentRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AppFrontendProperties frontendProperties;

    public PasswordResetService(
        UserRepository userRepository,
        PasswordResetTokenRepository passwordResetTokenRepository,
        AdminRepository adminRepository,
        ParentRepository parentRepository,
        TeacherRepository teacherRepository,
        PasswordEncoder passwordEncoder,
        EmailService emailService,
        AppFrontendProperties frontendProperties) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.adminRepository = adminRepository;
        this.parentRepository = parentRepository;
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.frontendProperties = frontendProperties;
    }

    @Transactional
    public String issueResetToken(User user, boolean requireChangeOnLogin) {
        String rawToken = UUID.randomUUID().toString();
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setTokenHash(TokenHashUtil.sha256(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusHours(24));
        passwordResetTokenRepository.save(token);

        if (requireChangeOnLogin) {
            user.setMustChangePassword(true);
            userRepository.save(user);
        }
        return rawToken;
    }

    public void sendResetEmail(User user, String rawToken, String recipientName) {
        String resetUrl = buildResetUrl(rawToken);
        emailService.sendPasswordReset(user.getEmail(), recipientName, resetUrl);
    }

    @Transactional
    public void initiateForgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                return;
            }
            String rawToken = issueResetToken(user, false);
            sendForgotPasswordEmail(user, rawToken, resolveDisplayName(user));
        });
    }

    public void sendForgotPasswordEmail(User user, String rawToken, String recipientName) {
        String resetUrl = buildResetUrl(rawToken);
        emailService.sendForgotPasswordReset(user.getEmail(), recipientName, resetUrl);
    }

    private String buildResetUrl(String rawToken) {
        return frontendProperties.getOrigin().replaceAll("/$", "")
            + "/reset-password?token=" + rawToken;
    }

    private String resolveDisplayName(User user) {
        return switch (user.getRole()) {
            case TEACHER -> teacherRepository.findByUserId(user.getId())
                .map(Teacher::getFullName).orElse(user.getEmail());
            case PARENT -> parentRepository.findByUserId(user.getId())
                .map(Parent::getFullName).orElse(user.getEmail());
            case ADMIN -> adminRepository.findByUserId(user.getId())
                .map(Admin::getFullName).orElse(user.getEmail());
        };
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String hash = TokenHashUtil.sha256(rawToken);
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(hash)
            .orElseThrow(() -> new UnauthorizedException("Invalid or expired reset token"));
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Invalid or expired reset token");
        }
        User user = userRepository.findById(token.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ConflictException("User account is deactivated");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setAccessTokenVersion(user.getAccessTokenVersion() + 1);
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);
    }
}
