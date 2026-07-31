package com.example.backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.entity.Admin;
import com.example.backend.entity.User;
import com.example.backend.repository.AdminRepository;
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

@SpringBootTest(properties = {
    "app.seed.enabled=false",
    "app.security.rate-limit.enabled=true",
    "app.security.rate-limit.auth-requests-per-minute=2",
    "app.security.require-https=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityHardeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("secure@school.local");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(User.Role.ADMIN);
        user.setIsActive(true);
        user = userRepository.save(user);

        Admin admin = new Admin();
        admin.setUserId(user.getId());
        admin.setFullName("Secure Admin");
        adminRepository.save(admin);
    }

    @Test
    void rateLimitsAuthEndpoints() throws Exception {
        String body = """
            {"email":"secure@school.local","password":"wrong"}
            """;
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.message").value("Rate limit exceeded. Please try again later."))
            .andExpect(header().string("Retry-After", "60"));
    }

}
