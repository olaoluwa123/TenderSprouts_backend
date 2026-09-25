package com.example.backend.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.entity.AcademicSession;
import com.example.backend.entity.Admin;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.User;
import com.example.backend.repository.AcademicSessionRepository;
import com.example.backend.repository.AdminRepository;
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentCsvImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private AcademicSessionRepository academicSessionRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private ParentStudentRepository parentStudentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String className;

    @BeforeEach
    void setUp() throws Exception {
        String adminEmail = "csv-import-admin-" + UUID.randomUUID() + "@school.local";
        User adminUser = new User();
        adminUser.setEmail(adminEmail);
        adminUser.setPassword(passwordEncoder.encode("password123"));
        adminUser.setRole(User.Role.ADMIN);
        adminUser.setIsActive(true);
        adminUser = userRepository.save(adminUser);

        Admin admin = new Admin();
        admin.setUserId(adminUser.getId());
        admin.setFullName("CSV Admin");
        adminRepository.save(admin);

        AcademicSession session = new AcademicSession();
        session.setName("2026/2027");
        session.setStartDate(LocalDate.of(2026, 9, 1));
        session.setEndDate(LocalDate.of(2027, 7, 31));
        session.setIsActive(true);
        academicSessionRepository.save(session);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName("JSS 1A");
        schoolClass = schoolClassRepository.save(schoolClass);
        className = schoolClass.getName();

        adminToken = login(adminEmail);
    }

    @Test
    void importCsvOnboardsStudentsAndReusesParentForSiblingRows() throws Exception {
        String csv = """
            student_first_name,student_last_name,admission_number,class,gender,date_of_birth,parent_email,parent_full_name,parent_phone
            Ada,Okonkwo,CSV-001,%s,FEMALE,2012-03-15,sibling.parent@school.local,Mrs Sibling Parent,08011111111
            Chidi,Okonkwo,CSV-002,%s,MALE,2010-07-22,sibling.parent@school.local,Mrs Sibling Parent,08011111111
            """.stripIndent().formatted(className, className);

        long jobId = enqueueImport(csv);

        mockMvc.perform(get("/api/students/import-csv/jobs/" + jobId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.totalRows").value(2))
            .andExpect(jsonPath("$.successCount").value(2))
            .andExpect(jsonPath("$.failureCount").value(0))
            .andExpect(jsonPath("$.rows[0].status").value("IMPORTED"))
            .andExpect(jsonPath("$.rows[1].status").value("IMPORTED"))
            .andExpect(jsonPath("$.rows[1].message").value("Imported (linked existing parent)"));

        org.junit.jupiter.api.Assertions.assertTrue(
            studentRepository.findByAdmissionNumber("CSV-001").isPresent());
        org.junit.jupiter.api.Assertions.assertTrue(
            studentRepository.findByAdmissionNumber("CSV-002").isPresent());
        User parentUser = userRepository.findByEmail("sibling.parent@school.local").orElseThrow();
        long parentId = parentRepository.findByUserId(parentUser.getId()).orElseThrow().getId();
        org.junit.jupiter.api.Assertions.assertEquals(2, parentStudentRepository.findByParentId(parentId).size());
    }

    @Test
    void importCsvFailsRowForUnknownClass() throws Exception {
        String csv = """
            student_first_name,student_last_name,admission_number,class,gender,date_of_birth,parent_email,parent_full_name,parent_phone
            Ada,Okonkwo,CSV-003,Unknown Class,FEMALE,,parent3@school.local,Parent Three,
            """.stripIndent();

        long jobId = enqueueImport(csv);

        mockMvc.perform(get("/api/students/import-csv/jobs/" + jobId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.successCount").value(0))
            .andExpect(jsonPath("$.failureCount").value(1))
            .andExpect(jsonPath("$.rows[0].status").value("FAILED"))
            .andExpect(jsonPath("$.rows[0].message").value("Class not found: Unknown Class"));
    }

    @Test
    void importCsvRejectsDuplicateAdmissionInFile() throws Exception {
        String csv = """
            student_first_name,student_last_name,admission_number,class,gender,date_of_birth,parent_email,parent_full_name,parent_phone
            Ada,Okonkwo,CSV-DUP,%s,FEMALE,,dup1@school.local,Parent One,
            Chidi,Okonkwo,CSV-DUP,%s,MALE,,dup2@school.local,Parent Two,
            """.stripIndent().formatted(className, className);

        long jobId = enqueueImport(csv);

        mockMvc.perform(get("/api/students/import-csv/jobs/" + jobId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.successCount").value(1))
            .andExpect(jsonPath("$.failureCount").value(1))
            .andExpect(jsonPath("$.rows[1].message").value("Duplicate admission number in file: CSV-DUP"));
    }

    @Test
    void importCsvRejectsMissingRequiredHeader() throws Exception {
        String csv = """
            student_first_name,student_last_name,admission_number,class
            Ada,Okonkwo,CSV-004,%s
            """.stripIndent().formatted(className);

        mockMvc.perform(multipart("/api/students/import-csv")
                .file(csvFile(csv))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Missing required CSV column: parent_email"));
    }

    private long enqueueImport(String csv) throws Exception {
        MvcResult accepted = mockMvc.perform(multipart("/api/students/import-csv")
                .file(csvFile(csv))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.jobId").isNumber())
            .andReturn();
        JsonNode body = objectMapper.readTree(accepted.getResponse().getContentAsString());
        return body.get("jobId").asLong();
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
            "file",
            "students.csv",
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
