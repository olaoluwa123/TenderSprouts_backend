package com.example.backend.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.entity.Admin;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.User;
import com.example.backend.repository.AdminRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherCsvImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private TeacherClassRepository teacherClassRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String className;

    @BeforeEach
    void setUp() throws Exception {
        String adminEmail = "teacher-csv-admin-" + UUID.randomUUID() + "@school.local";
        User adminUser = new User();
        adminUser.setEmail(adminEmail);
        adminUser.setPassword(passwordEncoder.encode("password123"));
        adminUser.setRole(User.Role.ADMIN);
        adminUser.setIsActive(true);
        adminUser = userRepository.save(adminUser);

        Admin admin = new Admin();
        admin.setUserId(adminUser.getId());
        admin.setFullName("Teacher CSV Admin");
        adminRepository.save(admin);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName("CSV Class " + UUID.randomUUID().toString().substring(0, 8));
        schoolClass = schoolClassRepository.save(schoolClass);
        className = schoolClass.getName();

        adminToken = login(adminEmail);
    }

    @Test
    void importCsvOnboardsTeachersWithMustChangePassword() throws Exception {
        String email1 = "csv.teacher1." + UUID.randomUUID() + "@school.local";
        String email2 = "csv.teacher2." + UUID.randomUUID() + "@school.local";
        String csv = """
            email,full_name,phone
            %s,Ada Teacher,08011111111
            %s,Chidi Teacher,
            """.stripIndent().formatted(email1, email2);

        mockMvc.perform(multipart("/api/teachers/import-csv")
                .file(csvFile(csv))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRows").value(2))
            .andExpect(jsonPath("$.successCount").value(2))
            .andExpect(jsonPath("$.failureCount").value(0))
            .andExpect(jsonPath("$.rows[0].status").value("IMPORTED"))
            .andExpect(jsonPath("$.rows[1].status").value("IMPORTED"));

        User user1 = userRepository.findByEmail(email1).orElseThrow();
        User user2 = userRepository.findByEmail(email2).orElseThrow();
        Assertions.assertEquals(User.Role.TEACHER, user1.getRole());
        Assertions.assertTrue(Boolean.TRUE.equals(user1.getMustChangePassword()));
        Assertions.assertTrue(Boolean.TRUE.equals(user2.getMustChangePassword()));
        Assertions.assertTrue(teacherRepository.findAll().stream()
            .anyMatch(t -> user1.getId().equals(t.getUserId()) && "Ada Teacher".equals(t.getFullName())));
        Assertions.assertTrue(teacherRepository.findAll().stream()
            .anyMatch(t -> user2.getId().equals(t.getUserId()) && "Chidi Teacher".equals(t.getFullName())));
    }

    @Test
    void importCsvRejectsDuplicateEmailInFile() throws Exception {
        String email = "dup.teacher." + UUID.randomUUID() + "@school.local";
        String csv = """
            email,full_name,phone
            %s,First Teacher,08022222222
            %s,Second Teacher,08033333333
            """.stripIndent().formatted(email, email);

        mockMvc.perform(multipart("/api/teachers/import-csv")
                .file(csvFile(csv))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.successCount").value(1))
            .andExpect(jsonPath("$.failureCount").value(1))
            .andExpect(jsonPath("$.rows[1].status").value("FAILED"))
            .andExpect(jsonPath("$.rows[1].message").value("Duplicate email in file: " + email));
    }

    @Test
    void importCsvFailsRowForExistingEmail() throws Exception {
        String existingEmail = "existing.teacher." + UUID.randomUUID() + "@school.local";
        User existing = new User();
        existing.setEmail(existingEmail);
        existing.setPassword(passwordEncoder.encode("password123"));
        existing.setRole(User.Role.TEACHER);
        existing.setIsActive(true);
        existing = userRepository.save(existing);

        Teacher teacher = new Teacher();
        teacher.setUserId(existing.getId());
        teacher.setFullName("Existing Teacher");
        teacher.setIsActive(true);
        teacherRepository.save(teacher);

        String csv = """
            email,full_name,phone
            %s,Clash Teacher,08044444444
            """.stripIndent().formatted(existingEmail);

        mockMvc.perform(multipart("/api/teachers/import-csv")
                .file(csvFile(csv))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.successCount").value(0))
            .andExpect(jsonPath("$.failureCount").value(1))
            .andExpect(jsonPath("$.rows[0].status").value("FAILED"));
    }

    @Test
    void importCsvFailsRowForInvalidEmail() throws Exception {
        String csv = """
            email,full_name,phone
            not-an-email,Bad Email Teacher,08055555555
            """.stripIndent();

        mockMvc.perform(multipart("/api/teachers/import-csv")
                .file(csvFile(csv))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.successCount").value(0))
            .andExpect(jsonPath("$.failureCount").value(1))
            .andExpect(jsonPath("$.rows[0].status").value("FAILED"))
            .andExpect(jsonPath("$.rows[0].message").value("Invalid email: not-an-email"));
    }

    @Test
    void importCsvRejectsMissingRequiredHeader() throws Exception {
        String csv = """
            email,phone
            teacher@school.local,08066666666
            """.stripIndent();

        mockMvc.perform(multipart("/api/teachers/import-csv")
                .file(csvFile(csv))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Missing required CSV column: full_name"));
    }

    @Test
    void importCsvAssignsClassWhenProvided() throws Exception {
        String email = "csv.class.teacher." + UUID.randomUUID() + "@school.local";
        String csv = """
            email,full_name,phone,class
            %s,Class Teacher,08077777777,%s
            """.stripIndent().formatted(email, className);

        mockMvc.perform(multipart("/api/teachers/import-csv")
                .file(csvFile(csv))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.successCount").value(1))
            .andExpect(jsonPath("$.rows[0].status").value("IMPORTED"))
            .andExpect(jsonPath("$.rows[0].message").value("Imported - assigned to class, welcome email sent"));

        User user = userRepository.findByEmail(email).orElseThrow();
        Teacher teacher = teacherRepository.findByUserId(user.getId()).orElseThrow();
        Assertions.assertEquals(1, teacherClassRepository.findByTeacherIdAndIsActiveTrue(teacher.getId()).size());
    }

    @Test
    void importCsvFailsRowForUnknownClass() throws Exception {
        String email = "csv.badclass." + UUID.randomUUID() + "@school.local";
        String csv = """
            email,full_name,phone,class
            %s,Lost Teacher,08088888888,Unknown Class XYZ
            """.stripIndent().formatted(email);

        mockMvc.perform(multipart("/api/teachers/import-csv")
                .file(csvFile(csv))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.successCount").value(0))
            .andExpect(jsonPath("$.failureCount").value(1))
            .andExpect(jsonPath("$.rows[0].status").value("FAILED"))
            .andExpect(jsonPath("$.rows[0].message").value("Class not found: Unknown Class XYZ"));
    }

    @Test
    void onboardWithClassIdAssignsClass() throws Exception {
        String email = "onboard.class." + UUID.randomUUID() + "@school.local";
        Long classId = schoolClassRepository.findFirstByNameIgnoreCase(className).orElseThrow().getId();

        mockMvc.perform(post("/api/teachers/onboard")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email":"%s",
                      "fullName":"Onboard With Class",
                      "phone":"08099999999",
                      "classId":%d
                    }
                    """.formatted(email, classId)))
            .andExpect(status().isCreated());

        User user = userRepository.findByEmail(email).orElseThrow();
        Teacher teacher = teacherRepository.findByUserId(user.getId()).orElseThrow();
        Assertions.assertEquals(1, teacherClassRepository.findByTeacherIdAndIsActiveTrue(teacher.getId()).size());
        Assertions.assertTrue(Boolean.TRUE.equals(user.getMustChangePassword()));
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
            "file",
            "teachers.csv",
            "text/csv",
            content.getBytes(StandardCharsets.UTF_8));
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
