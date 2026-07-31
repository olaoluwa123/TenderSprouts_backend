package com.example.backend.tier2;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.entity.Admin;
import com.example.backend.entity.User;
import com.example.backend.repository.AdminRepository;
import com.example.backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Tier2IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        User user = new User();
        user.setEmail("tier2-admin@school.local");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(User.Role.ADMIN);
        user.setIsActive(true);
        user = userRepository.save(user);

        Admin admin = new Admin();
        admin.setUserId(user.getId());
        admin.setFullName("Tier2 Admin");
        adminRepository.save(admin);

        var loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"tier2-admin@school.local","password":"password123"}
                    """))
            .andExpect(status().isOk())
            .andReturn();
        accessToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");
    }

    @Test
    void assessmentTypesList() throws Exception {
        mockMvc.perform(get("/api/assessment-types")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].code").value("CA"));
    }
}
