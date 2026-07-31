package com.example.backend.academic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.entity.AcademicSession;
import com.example.backend.entity.Admin;
import com.example.backend.entity.ClassSubject;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Student;
import com.example.backend.entity.StudentEnrollment;
import com.example.backend.entity.Subject;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.TeacherClass;
import com.example.backend.entity.Term;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.EnrollmentStatus;
import com.example.backend.repository.AcademicSessionRepository;
import com.example.backend.repository.AdminRepository;
import com.example.backend.repository.ClassSubjectRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentEnrollmentRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
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
class ActiveTermIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AcademicSessionRepository academicSessionRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ClassSubjectRepository classSubjectRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TeacherClassRepository teacherClassRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String teacherToken;
    private long sessionId;
    private long firstTermId;
    private long secondTermId;
    private long classId;
    private long subjectId;
    private long studentId;

    @BeforeEach
    void setUp() throws Exception {
        User adminUser = saveUser("term-admin@school.local", User.Role.ADMIN);
        Admin admin = new Admin();
        admin.setUserId(adminUser.getId());
        admin.setFullName("Term Admin");
        adminRepository.save(admin);

        AcademicSession session = new AcademicSession();
        session.setName("2026/2027");
        session.setStartDate(LocalDate.of(2026, 9, 1));
        session.setEndDate(LocalDate.of(2027, 7, 31));
        session.setIsActive(true);
        session = academicSessionRepository.save(session);
        sessionId = session.getId();

        Term first = new Term();
        first.setSessionId(sessionId);
        first.setName("First Term");
        first.setIsActive(true);
        first = termRepository.save(first);
        firstTermId = first.getId();

        Term second = new Term();
        second.setSessionId(sessionId);
        second.setName("Second Term");
        second.setIsActive(false);
        second = termRepository.save(second);
        secondTermId = second.getId();

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName("JSS 1");
        schoolClass = schoolClassRepository.save(schoolClass);
        classId = schoolClass.getId();

        Subject subject = new Subject();
        subject.setName("Math");
        subject = subjectRepository.save(subject);
        subjectId = subject.getId();

        ClassSubject classSubject = new ClassSubject();
        classSubject.setClassId(classId);
        classSubject.setSubjectId(subjectId);
        classSubjectRepository.save(classSubject);

        User teacherUser = saveUser("term-teacher@school.local", User.Role.TEACHER);
        Teacher teacher = new Teacher();
        teacher.setUserId(teacherUser.getId());
        teacher.setFullName("Term Teacher");
        long teacherId = teacherRepository.save(teacher).getId();

        TeacherClass teacherClass = new TeacherClass();
        teacherClass.setTeacherId(teacherId);
        teacherClass.setClassId(classId);
        teacherClassRepository.save(teacherClass);

        Student student = new Student();
        student.setFirstName("Active");
        student.setLastName("Term");
        student.setAdmissionNumber("ADM-AT-001");
        student.setClassId(classId);
        student.setIsActive(true);
        student = studentRepository.save(student);
        studentId = student.getId();

        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setClassId(classId);
        enrollment.setSessionId(sessionId);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setStartDate(LocalDate.now());
        studentEnrollmentRepository.save(enrollment);

        adminToken = login("term-admin@school.local");
        teacherToken = login("term-teacher@school.local");
    }

    @Test
    void adminActivatesTermAndOnlyOneRemainsActive() throws Exception {
        mockMvc.perform(get("/api/terms/active")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(firstTermId))
            .andExpect(jsonPath("$.name").value("First Term"))
            .andExpect(jsonPath("$.isActive").value(true));

        mockMvc.perform(post("/api/sessions/" + sessionId + "/terms/" + secondTermId + "/activate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(secondTermId))
            .andExpect(jsonPath("$.isActive").value(true));

        mockMvc.perform(get("/api/terms/active")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(secondTermId));

        long activeCount = termRepository.findAll().stream()
            .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
            .count();
        assertThat(activeCount).isEqualTo(1);
    }

    @Test
    void activeTermMustBelongToActiveSession() throws Exception {
        AcademicSession other = new AcademicSession();
        other.setName("Old session");
        other.setStartDate(LocalDate.of(2024, 9, 1));
        other.setEndDate(LocalDate.of(2025, 7, 31));
        other.setIsActive(false);
        other = academicSessionRepository.save(other);

        Term stale = new Term();
        stale.setSessionId(other.getId());
        stale.setName("Stale Term");
        stale.setIsActive(true);
        termRepository.save(stale);

        Term inActiveSession = termRepository.findFirstBySessionIdAndIsActiveTrue(sessionId).orElseThrow();
        inActiveSession.setIsActive(false);
        termRepository.save(inActiveSession);

        mockMvc.perform(get("/api/terms/active")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void teacherCanReadActiveTerm() throws Exception {
        mockMvc.perform(get("/api/terms/active")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(firstTermId));
    }

    @Test
    void teacherCannotActivateTerm() throws Exception {
        mockMvc.perform(post("/api/sessions/" + sessionId + "/terms/" + secondTermId + "/activate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotUpsertGradesForInactiveTerm() throws Exception {
        mockMvc.perform(put("/api/students/" + studentId + "/grades")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "termId":%d,
                      "classId":%d,
                      "entries":[
                        {"subjectId":%d,"assessmentType":"CA","score":20}
                      ]
                    }
                    """.formatted(secondTermId, classId, subjectId)))
            .andExpect(status().isConflict());
    }

    @Test
    void sessionActivateResetsActiveTermToFirstTerm() throws Exception {
        mockMvc.perform(post("/api/sessions/" + sessionId + "/terms/" + secondTermId + "/activate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk());

        AcademicSession other = new AcademicSession();
        other.setName("2027/2028");
        other.setStartDate(LocalDate.of(2027, 9, 1));
        other.setEndDate(LocalDate.of(2028, 7, 31));
        other.setIsActive(false);
        other = academicSessionRepository.save(other);

        Term otherFirst = new Term();
        otherFirst.setSessionId(other.getId());
        otherFirst.setName("First Term");
        otherFirst.setIsActive(false);
        termRepository.save(otherFirst);

        mockMvc.perform(post("/api/sessions/" + other.getId() + "/activate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/terms/active")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value(other.getId()))
            .andExpect(jsonPath("$.name").value("First Term"));
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
