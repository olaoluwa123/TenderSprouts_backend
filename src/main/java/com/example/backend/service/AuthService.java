package com.example.backend.service;

import com.example.backend.config.AuthProperties;
import com.example.backend.config.JwtProperties;
import com.example.backend.dto.auth.ChangePasswordRequest;
import com.example.backend.dto.auth.ForgotPasswordRequest;
import com.example.backend.dto.auth.LoginRequest;
import com.example.backend.dto.auth.RefreshRequest;
import com.example.backend.dto.auth.TokenResponse;
import com.example.backend.entity.LoginAttempt;
import com.example.backend.entity.RefreshToken;
import com.example.backend.entity.User;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.LoginAttemptRepository;
import com.example.backend.repository.PasswordResetTokenRepository;
import com.example.backend.repository.RefreshTokenRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.ClientIpResolver;
import com.example.backend.security.CustomUserDetailsService;
import com.example.backend.security.JwtService;
import com.example.backend.security.SecurityUser;
import com.example.backend.security.TokenHashUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthProperties authProperties;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordResetService passwordResetService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
        AuthenticationManager authenticationManager,
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        LoginAttemptRepository loginAttemptRepository,
        PasswordResetTokenRepository passwordResetTokenRepository,
        JwtService jwtService,
        JwtProperties jwtProperties,
        AuthProperties authProperties,
        CustomUserDetailsService userDetailsService,
        PasswordResetService passwordResetService,
        PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.authProperties = authProperties;
        this.userDetailsService = userDetailsService;
        this.passwordResetService = passwordResetService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TokenResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = ClientIpResolver.resolve(httpRequest);
        checkLockout(request.email(), clientIp);
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException ex) {
            recordAttempt(request.email(), clientIp, false);
            throw new UnauthorizedException("Invalid email or password");
        }
        recordAttempt(request.email(), clientIp, true);
        User user = userRepository.findByEmail(request.email()).orElseThrow();
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        SecurityUser securityUser = userDetailsService.loadSecurityUserById(user.getId());
        validateProfileForRole(securityUser);
        return issueTokens(securityUser);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        String hash = TokenHashUtil.sha256(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash)
            .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }
        stored.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(stored);
        SecurityUser user = userDetailsService.loadSecurityUserById(stored.getUserId());
        return issueTokens(user);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        String hash = TokenHashUtil.sha256(request.refreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash)
            .orElse(null);
        if (refreshToken == null) {
            return;
        }
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        User user = userRepository.findById(refreshToken.getUserId()).orElseThrow();
        user.setAccessTokenVersion(user.getAccessTokenVersion() + 1);
        userRepository.save(user);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        passwordResetService.initiateForgotPassword(request.email());
    }

    @Transactional
    public void resetPassword(com.example.backend.dto.auth.ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
    }

    @Transactional
    public TokenResponse changePassword(ChangePasswordRequest request, SecurityUser actor) {
        User user = userRepository.findById(actor.getUserId())
            .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setAccessTokenVersion(user.getAccessTokenVersion() + 1);
        userRepository.save(user);
        SecurityUser securityUser = userDetailsService.loadSecurityUserById(user.getId());
        return issueTokens(securityUser);
    }

    private TokenResponse issueTokens(SecurityUser user) {
        User userEntity = userRepository.findById(user.getUserId()).orElseThrow();
        String access = jwtService.generateAccessToken(user, userEntity.getAccessTokenVersion());
        String rawRefresh = UUID.randomUUID().toString();
        RefreshToken refresh = new RefreshToken();
        refresh.setUserId(user.getUserId());
        refresh.setTokenHash(TokenHashUtil.sha256(rawRefresh));
        refresh.setExpiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshTokenExpirationMs() / 1000));
        refreshTokenRepository.save(refresh);
        return TokenResponse.of(
            access,
            rawRefresh,
            jwtProperties.getAccessTokenExpirationMs(),
            user.getUserId(),
            user.getRole(),
            user.getProfileId(),
            Boolean.TRUE.equals(userEntity.getMustChangePassword()));
    }

    private void validateProfileForRole(SecurityUser user) {
        if (user.getProfileId() == null) {
            throw new UnauthorizedException("User profile is not configured for role " + user.getRole());
        }
    }

    private void checkLockout(String email, String clientIp) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(authProperties.getLockoutMinutes());
        long failuresByEmail = loginAttemptRepository.countFailedAttemptsSince(email, since);
        if (failuresByEmail >= authProperties.getMaxFailedAttempts()) {
            throw new UnauthorizedException("Account temporarily locked. Try again later.");
        }
        if (clientIp != null && !clientIp.isBlank()) {
            long failuresByIp = loginAttemptRepository.countFailedAttemptsByIpSince(clientIp, since);
            if (failuresByIp >= authProperties.getMaxFailedAttemptsPerIp()) {
                throw new UnauthorizedException("Too many failed login attempts from this network. Try again later.");
            }
        }
    }

    private void recordAttempt(String email, String clientIp, boolean success) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setEmail(email);
        attempt.setSuccess(success);
        attempt.setAttemptedAt(LocalDateTime.now());
        attempt.setIpAddress(clientIp);
        loginAttemptRepository.save(attempt);
    }
}
