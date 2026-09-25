package com.example.backend.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.entity.AcademicSession;
import com.example.backend.entity.Admin;
import com.example.backend.entity.Parent;
import com.example.backend.entity.PasswordResetToken;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Student;
import com.example.backend.entity.StudentEnrollment;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.EnrollmentStatus;
import com.example.backend.repository.AcademicSessionRepository;
import com.example.backend.repository.AdminRepository;
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.PasswordResetTokenRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentEnrollmentRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.entity.TeacherClass;
import com.example.backend.security.TokenHashUtil;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.UUID;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminFeaturesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private AcademicSessionRepository academicSessionRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private TeacherClassRepository teacherClassRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private long adminUserId;
    private long parentUserId;
    private long studentId;
    private long classAId;
    private long classBId;
    private long session2026Id;
    private long session2027Id;

    @BeforeEach
    void setUp() throws Exception {
        User adminUser = saveUser("admin-features@school.local", User.Role.ADMIN);
        adminUserId = adminUser.getId();
        Admin admin = new Admin();
        admin.setUserId(adminUser.getId());
        admin.setFullName("Admin");
        adminRepository.save(admin);

        User parentUser = saveUser("parent-features@school.local", User.Role.PARENT);
        parentUserId = parentUser.getId();
        Parent parent = new Parent();
        parent.setUserId(parentUser.getId());
        parent.setFullName("Parent");
        parentRepository.save(parent);

        AcademicSession session2026 = new AcademicSession();
        session2026.setName("2026/2027");
        session2026.setStartDate(LocalDate.of(2026, 9, 1));
        session2026.setEndDate(LocalDate.of(2027, 7, 31));
        session2026.setIsActive(true);
        session2026 = academicSessionRepository.save(session2026);
        session2026Id = session2026.getId();

        AcademicSession session2027 = new AcademicSession();
        session2027.setName("2027/2028");
        session2027.setStartDate(LocalDate.of(2027, 9, 1));
        session2027.setEndDate(LocalDate.of(2028, 7, 31));
        session2027.setIsActive(false);
        session2027 = academicSessionRepository.save(session2027);
        session2027Id = session2027.getId();

        SchoolClass classA = new SchoolClass();
        classA.setName("JSS 1A");
        classA = schoolClassRepository.save(classA);
        classAId = classA.getId();

        SchoolClass classB = new SchoolClass();
        classB.setName("JSS 2A");
        classB = schoolClassRepository.save(classB);
        classBId = classB.getId();

        Student student = new Student();
        student.setFirstName("Promote");
        student.setLastName("Me");
        student.setAdmissionNumber("ADM-PROMO-001");
        student.setClassId(classAId);
        student.setIsActive(true);
        student = studentRepository.save(student);
        studentId = student.getId();

        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setClassId(classAId);
        enrollment.setSessionId(session2026Id);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setStartDate(LocalDate.now());
        enrollment = studentEnrollmentRepository.save(enrollment);
        student.setCurrentEnrollmentId(enrollment.getId());
        studentRepository.save(student);

        adminToken = login("admin-features@school.local");
    }

    @Test
    void deactivateUserBlocksLoginAndReactivateRestores() throws Exception {
        mockMvc.perform(patch("/api/users/" + parentUserId + "/status")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isActive\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isActive").value(false));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"parent-features@school.local","password":"password123"}
                    """))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/users/" + parentUserId + "/status")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isActive\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isActive").value(true));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"parent-features@school.local","password":"password123"}
                    """))
            .andExpect(status().isOk());
    }

    @Test
    void onboardStudentWithParentCreatesBothAndLinks() throws Exception {
        mockMvc.perform(post("/api/students/onboard")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName":"Combined",
                      "lastName":"Student",
                      "admissionNumber":"ADM-COMBINED-001",
                      "gender":"MALE",
                      "classId":%d,
                      "parentEmail":"combined-parent@school.local",
                      "parentFullName":"Combined Parent",
                      "parentPhone":"555-0100"
                    }
                    """.formatted(classAId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.student.firstName").value("Combined"))
            .andExpect(jsonPath("$.parent.email").value("combined-parent@school.local"));

        User user = userRepository.findByEmail("combined-parent@school.local").orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(user.getMustChangePassword());
    }

    @Test
    void onboardParentForStudentSendsCredentialsWithoutResetToken() throws Exception {
        mockMvc.perform(post("/api/students/" + studentId + "/onboard-parent")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email":"new-onboard-parent@school.local",
                      "fullName":"Onboard Parent",
                      "phone":"555-0199"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("new-onboard-parent@school.local"));

        User user = userRepository.findByEmail("new-onboard-parent@school.local").orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(user.getMustChangePassword());
        org.junit.jupiter.api.Assertions.assertTrue(
            passwordResetTokenRepository.findAll().stream()
                .noneMatch(t -> t.getUserId().equals(user.getId()) && t.getUsedAt() == null));
    }

    @Test
    void teachersAndClassesCanHaveManyActiveAssignments() throws Exception {
        MvcResult teacherAResult = mockMvc.perform(post("/api/teachers/onboard")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email":"assign-teacher-a@school.local",
                      "fullName":"Assign Teacher A",
                      "phone":"555-0300"
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn();
        long teacherAId = ((Number) JsonPath.read(
            teacherAResult.getResponse().getContentAsString(), "$.id")).longValue();

        MvcResult teacherBResult = mockMvc.perform(post("/api/teachers/onboard")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email":"assign-teacher-b@school.local",
                      "fullName":"Assign Teacher B",
                      "phone":"555-0301"
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn();
        long teacherBId = ((Number) JsonPath.read(
            teacherBResult.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(post("/api/teachers/" + teacherAId + "/classes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .param("classId", String.valueOf(classAId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.classId").value(classAId));

        mockMvc.perform(post("/api/teachers/" + teacherAId + "/classes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .param("classId", String.valueOf(classBId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.classId").value(classBId));

        mockMvc.perform(get("/api/teachers/" + teacherAId + "/classes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(post("/api/teachers/" + teacherBId + "/classes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .param("classId", String.valueOf(classAId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.classId").value(classAId));

        mockMvc.perform(get("/api/classes/" + classAId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.teachers.length()").value(2));

        mockMvc.perform(post("/api/teachers/" + teacherAId + "/classes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .param("classId", String.valueOf(classAId)))
            .andExpect(status().isConflict());

        mockMvc.perform(post("/api/teachers/" + teacherAId + "/classes/unassign")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .param("classId", String.valueOf(classAId)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/teachers/" + teacherAId + "/classes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].classId").value(classBId));
    }

    @Test
    void onboardTeacherCreatesUserWithMustChangePassword() throws Exception {
        mockMvc.perform(post("/api/teachers/onboard")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email":"new-teacher@school.local",
                      "fullName":"New Teacher",
                      "phone":"555-0200"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("new-teacher@school.local"))
            .andExpect(jsonPath("$.fullName").value("New Teacher"));

        User user = userRepository.findByEmail("new-teacher@school.local").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(User.Role.TEACHER, user.getRole());
        org.junit.jupiter.api.Assertions.assertTrue(user.getMustChangePassword());
    }

    @Test
    void resetPasswordClearsMustChangeFlag() throws Exception {
        String rawToken = UUID.randomUUID().toString();
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(parentUserId);
        token.setTokenHash(TokenHashUtil.sha256(rawToken));
        token.setExpiresAt(java.time.LocalDateTime.now().plusHours(24));
        passwordResetTokenRepository.save(token);

        User user = userRepository.findById(parentUserId).orElseThrow();
        user.setMustChangePassword(true);
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"token":"%s","newPassword":"resetpass99"}
                    """.formatted(rawToken)))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"parent-features@school.local","password":"resetpass99"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.passwordChangeRequired").value(false));
    }

    @Test
    void bulkPromoteMovesStudentsToNextClassAndSession() throws Exception {
        mockMvc.perform(post("/api/enrollments/bulk-promote")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sourceClassId":%d,
                      "sourceSessionId":%d,
                      "targetClassId":%d
                    }
                    """.formatted(classAId, session2026Id, classBId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.promoted").value(1))
            .andExpect(jsonPath("$.results[0].status").value("PROMOTED"));

        mockMvc.perform(get("/api/students/" + studentId + "/enrollments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].classId").value(classBId))
            .andExpect(jsonPath("$[0].sessionId").value(session2027Id));
    }

    private User saveUser(String email, User.Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(role);
        user.setIsActive(true);
        return userRepository.save(user);
    }

    private String login(String email) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"password123"}
                    """.formatted(email)))
            .andExpect(status().isOk())
            .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

}
