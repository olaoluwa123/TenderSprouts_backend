package com.example.backend.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.entity.AcademicSession;
import com.example.backend.entity.Admin;
import com.example.backend.entity.Parent;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Student;
import com.example.backend.entity.StudentEnrollment;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.TeacherClass;
import com.example.backend.entity.Term;
import com.example.backend.entity.TermResult;
import com.example.backend.entity.TermResultSubmission;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.EnrollmentStatus;
import com.example.backend.entity.enums.TermResultSubmissionStatus;
import com.example.backend.repository.AcademicSessionRepository;
import com.example.backend.repository.AdminRepository;
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentEnrollmentRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.repository.TermResultRepository;
import com.example.backend.repository.TermResultSubmissionRepository;
import com.example.backend.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardIntegrationTest {

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
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private AcademicSessionRepository academicSessionRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private TeacherClassRepository teacherClassRepository;

    @Autowired
    private TermResultRepository termResultRepository;

    @Autowired
    private TermResultSubmissionRepository termResultSubmissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String teacherToken;
    private String parentToken;
    private long classId;
    private long termId;
    private long sessionId;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User adminUser = saveUser("dash-admin-" + suffix + "@school.local", User.Role.ADMIN);
        Admin admin = new Admin();
        admin.setUserId(adminUser.getId());
        admin.setFullName("Dash Admin");
        adminRepository.save(admin);

        User teacherUser = saveUser("dash-teacher-" + suffix + "@school.local", User.Role.TEACHER);
        Teacher teacher = new Teacher();
        teacher.setUserId(teacherUser.getId());
        teacher.setFullName("Dash Teacher");
        teacher = teacherRepository.save(teacher);

        User parentUser = saveUser("dash-parent-" + suffix + "@school.local", User.Role.PARENT);
        Parent parent = new Parent();
        parent.setUserId(parentUser.getId());
        parent.setFullName("Dash Parent");
        parentRepository.save(parent);

        AcademicSession session = new AcademicSession();
        session.setName("2026/2027");
        session.setStartDate(LocalDate.of(2026, 9, 1));
        session.setEndDate(LocalDate.of(2027, 7, 31));
        session.setIsActive(true);
        session = academicSessionRepository.save(session);
        sessionId = session.getId();

        Term term = new Term();
        term.setSessionId(sessionId);
        term.setName("First Term");
        term.setIsActive(true);
        term = termRepository.save(term);
        termId = term.getId();

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName("JSS 1A");
        schoolClass = schoolClassRepository.save(schoolClass);
        classId = schoolClass.getId();

        SchoolClass otherClass = new SchoolClass();
        otherClass.setName("JSS 2A");
        schoolClassRepository.save(otherClass);

        Student student = new Student();
        student.setFirstName("Dash");
        student.setLastName("Student");
        student.setAdmissionNumber("DASH-" + suffix);
        student.setClassId(classId);
        student.setIsActive(true);
        student = studentRepository.save(student);

        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudentId(student.getId());
        enrollment.setClassId(classId);
        enrollment.setSessionId(sessionId);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setStartDate(LocalDate.now());
        enrollment = studentEnrollmentRepository.save(enrollment);
        student.setCurrentEnrollmentId(enrollment.getId());
        studentRepository.save(student);

        TeacherClass assignment = new TeacherClass();
        assignment.setTeacherId(teacher.getId());
        assignment.setClassId(classId);
        assignment.setIsActive(true);
        teacherClassRepository.save(assignment);

        TermResult result = new TermResult();
        result.setStudentId(student.getId());
        result.setClassId(classId);
        result.setTermId(termId);
        result.setSessionId(sessionId);
        result.setAverageScore(new BigDecimal("75.00"));
        result.setRankInClass(1);
        termResultRepository.save(result);

        TermResultSubmission submission = new TermResultSubmission();
        submission.setClassId(classId);
        submission.setTermId(termId);
        submission.setSessionId(sessionId);
        submission.setStatus(TermResultSubmissionStatus.SUBMITTED);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setSubmittedByTeacherId(teacher.getId());
        termResultSubmissionRepository.save(submission);

        adminToken = login(adminUser.getEmail());
        teacherToken = login(teacherUser.getEmail());
        parentToken = login(parentUser.getEmail());
    }

    @Test
    void adminDashboardReturnsKpisAndFunnel() throws Exception {
        mockMvc.perform(get("/api/dashboard/admin")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value(sessionId))
            .andExpect(jsonPath("$.termId").value(termId))
            .andExpect(jsonPath("$.kpis.activeStudents").value(1))
            .andExpect(jsonPath("$.kpis.activeTeachers").value(1))
            .andExpect(jsonPath("$.kpis.activeParents").value(1))
            .andExpect(jsonPath("$.kpis.classes").value(2))
            .andExpect(jsonPath("$.submissionFunnel.submitted").value(1))
            .andExpect(jsonPath("$.submissionFunnel.published").value(0))
            .andExpect(jsonPath("$.pendingApprovals.length()").value(1))
            .andExpect(jsonPath("$.pendingApprovals[0].className").value("JSS 1A"))
            .andExpect(jsonPath("$.classPerformance[0].className").value("JSS 1A"))
            .andExpect(jsonPath("$.gradeDistribution[0].letter").value("A"));
    }

    @Test
    void teacherDashboardIsScopedToAssignedClass() throws Exception {
        mockMvc.perform(get("/api/dashboard/teacher")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.classId").value(classId))
            .andExpect(jsonPath("$.className").value("JSS 1A"))
            .andExpect(jsonPath("$.studentCount").value(1))
            .andExpect(jsonPath("$.submissionStatus").value("SUBMITTED"))
            .andExpect(jsonPath("$.gradingProgress.nextAction").value("Waiting for admin approval"))
            .andExpect(jsonPath("$.topPerformers[0].studentName").value("Dash Student"));
    }

    @Test
    void teacherCanAccessTeacherDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/teacher")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
            .andExpect(status().isOk());
    }

    @Test
    void parentCannotAccessDashboards() throws Exception {
        mockMvc.perform(get("/api/dashboard/admin")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/dashboard/teacher")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
            .andExpect(status().isForbidden());
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
        String body = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"password123"}
                    """.formatted(email)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return body.replaceAll(".*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }
}
