package com.example.backend.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.entity.Admin;
import com.example.backend.entity.User;
import com.example.backend.repository.AdminRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
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

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CreateUserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        String adminEmail = "create-user-admin-" + UUID.randomUUID() + "@school.local";
        User adminUser = new User();
        adminUser.setEmail(adminEmail);
        adminUser.setPassword(passwordEncoder.encode("password123"));
        adminUser.setRole(User.Role.ADMIN);
        adminUser.setIsActive(true);
        adminUser = userRepository.save(adminUser);

        Admin admin = new Admin();
        admin.setUserId(adminUser.getId());
        admin.setFullName("Create User Admin");
        adminRepository.save(admin);

        adminToken = login(adminEmail);
    }

    @Test
    void createAdminPersistsProfileAndRequiresPasswordChange() throws Exception {
        String email = "new.admin." + UUID.randomUUID() + "@school.local";

        mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email":"%s",
                      "fullName":"New Admin",
                      "role":"ADMIN"
                    }
                    """.formatted(email)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.fullName").value("New Admin"))
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.mustChangePassword").value(true))
            .andExpect(jsonPath("$.isActive").value(true));

        User created = userRepository.findByEmail(email).orElseThrow();
        Assertions.assertEquals(User.Role.ADMIN, created.getRole());
        Assertions.assertTrue(Boolean.TRUE.equals(created.getMustChangePassword()));
        Assertions.assertTrue(adminRepository.findByUserId(created.getId()).isPresent());
        Assertions.assertEquals(
            "New Admin",
            adminRepository.findByUserId(created.getId()).orElseThrow().getFullName());
    }

    @Test
    void createTeacherPersistsProfileAndRequiresPasswordChange() throws Exception {
        String email = "new.teacher." + UUID.randomUUID() + "@school.local";

        mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email":"%s",
                      "fullName":"New Teacher",
                      "role":"TEACHER",
                      "phone":"08012345678"
                    }
                    """.formatted(email)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.role").value("TEACHER"))
            .andExpect(jsonPath("$.mustChangePassword").value(true));

        User created = userRepository.findByEmail(email).orElseThrow();
        Assertions.assertEquals(User.Role.TEACHER, created.getRole());
        Assertions.assertTrue(Boolean.TRUE.equals(created.getMustChangePassword()));
        Assertions.assertTrue(teacherRepository.findByUserId(created.getId()).isPresent());
        Assertions.assertEquals(
            "New Teacher",
            teacherRepository.findByUserId(created.getId()).orElseThrow().getFullName());
    }

    @Test
    void createRejectsParentRole() throws Exception {
        mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email":"parent.create@school.local",
                      "fullName":"Should Fail",
                      "role":"PARENT"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Only ADMIN and TEACHER users can be created here"));
    }

    @Test
    void createRejectsDuplicateEmail() throws Exception {
        String email = "dup.user." + UUID.randomUUID() + "@school.local";

        mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email":"%s",
                      "fullName":"First",
                      "role":"TEACHER"
                    }
                    """.formatted(email)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email":"%s",
                      "fullName":"Second",
                      "role":"ADMIN"
                    }
                    """.formatted(email)))
            .andExpect(status().isConflict());
    }

    private String login(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"password123"}
                    """.formatted(email)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }
}
