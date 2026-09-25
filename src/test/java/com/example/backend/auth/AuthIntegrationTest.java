package com.example.backend.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpHeaders;

import com.example.backend.entity.Admin;
import com.example.backend.entity.User;
import com.example.backend.repository.AdminRepository;
import com.example.backend.repository.PasswordResetTokenRepository;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("test@school.local");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(User.Role.ADMIN);
        user.setIsActive(true);
        user = userRepository.save(user);

        Admin admin = new Admin();
        admin.setUserId(user.getId());
        admin.setFullName("Test Admin");
        adminRepository.save(admin);
    }

    @Test
    void loginReturnsTokens() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"test@school.local","password":"password123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.profileId").isNumber());
    }

    @Test
    void logoutRevokesAccessToken() throws Exception {
        var loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"test@school.local","password":"password123"}
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        String accessToken = JsonPath.read(body, "$.accessToken");
        String refreshToken = JsonPath.read(body, "$.refreshToken");

        mockMvc.perform(get("/api/sessions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/sessions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loginFailsWithBadPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"test@school.local","password":"wrong"}
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPasswordCreatesResetTokenForKnownUser() throws Exception {
        User user = userRepository.findByEmail("test@school.local").orElseThrow();
        long tokensBefore = passwordResetTokenRepository.findAll().stream()
            .filter(token -> token.getUserId().equals(user.getId()))
            .count();

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"test@school.local"}
                    """))
            .andExpect(status().isAccepted());

        long tokensAfter = passwordResetTokenRepository.findAll().stream()
            .filter(token -> token.getUserId().equals(user.getId()))
            .count();
        org.junit.jupiter.api.Assertions.assertEquals(tokensBefore + 1, tokensAfter);
    }

    @Test
    void forgotPasswordDoesNotRevealUnknownEmail() throws Exception {
        long tokensBefore = passwordResetTokenRepository.count();

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"unknown@school.local"}
                    """))
            .andExpect(status().isAccepted());

        org.junit.jupiter.api.Assertions.assertEquals(tokensBefore, passwordResetTokenRepository.count());
    }

    @Test
    void changePasswordClearsMustChangeFlagAndReissuesTokens() throws Exception {
        User user = userRepository.findByEmail("test@school.local").orElseThrow();
        user.setMustChangePassword(true);
        user.setPassword(passwordEncoder.encode("temppass99"));
        userRepository.save(user);

        var loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"test@school.local","password":"temppass99"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.passwordChangeRequired").value(true))
            .andReturn();

        String accessToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/api/students")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword":"temppass99","newPassword":"newpass99"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.passwordChangeRequired").value(false));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"test@school.local","password":"newpass99"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.passwordChangeRequired").value(false));
    }

    @Test
    void loginLocksOutAfterRepeatedFailures() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"test@school.local","password":"wrong"}
                        """))
                .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"test@school.local","password":"password123"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Account temporarily locked. Try again later."));
    }
}
