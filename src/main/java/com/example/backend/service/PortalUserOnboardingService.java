package com.example.backend.service;

import com.example.backend.config.AppFrontendProperties;
import com.example.backend.entity.User;
import com.example.backend.exception.ConflictException;
import com.example.backend.repository.UserRepository;
import com.example.backend.util.TemporaryPasswordGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PortalUserOnboardingService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AppFrontendProperties frontendProperties;

    public PortalUserOnboardingService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        EmailService emailService,
        AppFrontendProperties frontendProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.frontendProperties = frontendProperties;
    }

    public OnboardedUser createUserWithTemporaryPassword(String email, User.Role role) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("Email already registered");
        }
        String plainPassword = TemporaryPasswordGenerator.generate();
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(plainPassword));
        user.setRole(role);
        user.setIsActive(true);
        user.setMustChangePassword(true);
        user = userRepository.save(user);
        return new OnboardedUser(user, plainPassword);
    }

    public void sendWelcomeCredentialsEmail(User user, String displayName, String roleLabel, String plainPassword) {
        String loginUrl = frontendProperties.getOrigin().replaceAll("/$", "") + "/login";
        emailService.sendWelcomeCredentials(user.getEmail(), displayName, roleLabel, plainPassword, loginUrl);
    }

    /** Welcome credentials for any role (parents and staff) via email. */
    public void sendWelcomeCredentials(
        User user,
        String displayName,
        String roleLabel,
        String plainPassword,
        String phone) {
        sendWelcomeCredentialsEmail(user, displayName, roleLabel, plainPassword);
    }

    public record OnboardedUser(User user, String plainPassword) {
    }
}
