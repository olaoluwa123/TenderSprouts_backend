package com.example.backend.academic;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.entity.AcademicSession;
import com.example.backend.entity.Admin;
import com.example.backend.entity.ClassSubject;
import com.example.backend.entity.Grade;
import com.example.backend.entity.Parent;
import com.example.backend.entity.ParentStudent;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Student;
import com.example.backend.entity.StudentEnrollment;
import com.example.backend.entity.Subject;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.Term;
import com.example.backend.entity.TeacherClass;
import com.example.backend.entity.TermResult;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.EnrollmentStatus;
import com.example.backend.repository.AcademicSessionRepository;
import com.example.backend.repository.AdminRepository;
import com.example.backend.repository.ClassSubjectRepository;
import com.example.backend.repository.GradeRepository;
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentEnrollmentRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TermResultRepository;
import com.example.backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class AcademicFeaturesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

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
    private ParentStudentRepository parentStudentRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ClassSubjectRepository classSubjectRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private TermResultRepository termResultRepository;

    @Autowired
    private TeacherClassRepository teacherClassRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String parentToken;
    private String teacherToken;
    private long studentId;
    private long classId;
    private long sessionId;
    private long termId;
    private long subjectId;
    private long teacherId;

    @BeforeEach
    void setUp() throws Exception {
        User adminUser = saveUser("academic-admin@school.local", User.Role.ADMIN);
        Admin admin = new Admin();
        admin.setUserId(adminUser.getId());
        admin.setFullName("Academic Admin");
        adminRepository.save(admin);

        User parentUser = saveUser("academic-parent@school.local", User.Role.PARENT);
        Parent parent = new Parent();
        parent.setUserId(parentUser.getId());
        parent.setFullName("Academic Parent");
        parent.setPhone("08012345678");
        parent = parentRepository.save(parent);

        AcademicSession session = new AcademicSession();
        session.setName("2026/2027");
        session.setStartDate(LocalDate.of(2026, 9, 1));
        session.setEndDate(LocalDate.of(2027, 7, 31));
        session.setIsActive(true);
        session = academicSessionRepository.save(session);
        sessionId = session.getId();

        Term term = new Term();
        term.setSessionId(sessionId);
        term.setName("Term 1");
        term.setIsActive(true);
        term = termRepository.save(term);
        termId = term.getId();

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName("SS 1A");
        schoolClass = schoolClassRepository.save(schoolClass);
        classId = schoolClass.getId();

        Subject subject = new Subject();
        subject.setName("English");
        subject = subjectRepository.save(subject);
        subjectId = subject.getId();

        ClassSubject classSubject = new ClassSubject();
        classSubject.setClassId(classId);
        classSubject.setSubjectId(subjectId);
        classSubjectRepository.save(classSubject);

        User teacherUser = saveUser("academic-teacher@school.local", User.Role.TEACHER);
        Teacher teacher = new Teacher();
        teacher.setUserId(teacherUser.getId());
        teacher.setFullName("Academic Teacher");
        teacherId = teacherRepository.save(teacher).getId();

        TeacherClass teacherClass = new TeacherClass();
        teacherClass.setTeacherId(teacherId);
        teacherClass.setClassId(classId);
        teacherClassRepository.save(teacherClass);

        Student student = new Student();
        student.setFirstName("Report");
        student.setLastName("Card");
        student.setAdmissionNumber("ADM-RC-001");
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
        enrollment = studentEnrollmentRepository.save(enrollment);
        student.setCurrentEnrollmentId(enrollment.getId());
        studentRepository.save(student);

        ParentStudent link = new ParentStudent();
        link.setParentId(parent.getId());
        link.setStudentId(studentId);
        parentStudentRepository.save(link);

        seedPublishedTermResult();

        adminToken = login("academic-admin@school.local");
        parentToken = login("academic-parent@school.local");
        teacherToken = login("academic-teacher@school.local");
    }

    @Test
    void parentDownloadsPublishedReportCardPdf() throws Exception {
        mockMvc.perform(get("/api/students/" + studentId + "/report-cards/" + termId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentName").value("Report Card"))
            .andExpect(jsonPath("$.averageScore").value(85.0))
            .andExpect(jsonPath("$.letterGrade").value("A"))
            .andExpect(jsonPath("$.subjectSummaries[0].subjectTotal").value(130.0))
            .andExpect(jsonPath("$.subjectSummaries[0].letterGrade").value("A"))
            .andExpect(jsonPath("$.subjectGrades.length()").value(2));

        mockMvc.perform(get("/api/students/" + studentId + "/report-cards/" + termId + "/pdf")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE));
    }

    @Test
    void teacherLoadsGradingSheetAndUpsertComputesAdditiveAverage() throws Exception {
        gradeRepository.deleteAll();
        termResultRepository.deleteAll();

        mockMvc.perform(get("/api/students/" + studentId + "/grading-sheet")
                .param("termId", String.valueOf(termId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentId").value(studentId))
            .andExpect(jsonPath("$.subjects.length()").value(1))
            .andExpect(jsonPath("$.assessmentTypes.length()").value(2));

        mockMvc.perform(put("/api/students/" + studentId + "/grades")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "termId":%d,
                      "classId":%d,
                      "entries":[
                        {"subjectId":%d,"assessmentType":"CA","score":25},
                        {"subjectId":%d,"assessmentType":"EXAM","score":55}
                      ]
                    }
                    """.formatted(termId, classId, subjectId, subjectId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subjects[0].total").value(80.0))
            .andExpect(jsonPath("$.subjects[0].letterGrade").value("A"));

        mockMvc.perform(post("/api/term-results/compute")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"classId":%d,"termId":%d,"sessionId":%d}
                    """.formatted(classId, termId, sessionId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].averageScore").value(80.0));
    }

    @Test
    void examTimetablePublishAndParentView() throws Exception {
        mockMvc.perform(post("/api/classes/" + classId + "/exam-timetable")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "subjectId":%d,
                      "termId":%d,
                      "examDate":"2026-11-15",
                      "startTime":"09:00:00",
                      "endTime":"11:00:00",
                      "room":"Hall A"
                    }
                    """.formatted(subjectId, termId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.subjectName").value("English"));

        mockMvc.perform(post("/api/classes/" + classId + "/exam-timetable/publish")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .param("termId", String.valueOf(termId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slotsPublished").value(1))
            .andExpect(jsonPath("$.parentsNotified").value(1));

        mockMvc.perform(get("/api/students/" + studentId + "/exam-timetable")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken)
                .param("termId", String.valueOf(termId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].room").value("Hall A"))
            .andExpect(jsonPath("$[0].publishedAt").isNotEmpty());
    }

    @Test
    void parentCannotAccessReportCardUntilAdminApproves() throws Exception {
        TermResult result = termResultRepository.findByStudentIdAndTermId(studentId, termId).orElseThrow();
        result.setPublishedAt(null);
        termResultRepository.save(result);

        mockMvc.perform(get("/api/students/" + studentId + "/report-cards/" + termId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/term-results/submit")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"classId":%d,"termId":%d,"sessionId":%d}
                    """.formatted(classId, termId, sessionId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SUBMITTED"))
            .andExpect(jsonPath("$.submittedAt").isNotEmpty());

        mockMvc.perform(get("/api/students/" + studentId + "/report-cards/" + termId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/term-results/approve")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"classId":%d,"termId":%d}
                    """.formatted(classId, termId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].publishedAt").isNotEmpty());

        mockMvc.perform(get("/api/students/" + studentId + "/report-cards/" + termId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentName").value("Report Card"));

        mockMvc.perform(get("/api/students/" + studentId + "/report-cards/" + termId + "/pdf")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE));
    }

    @Test
    void termResultPublishEmailsParent() throws Exception {
        TermResult result = termResultRepository.findByStudentIdAndTermId(studentId, termId).orElseThrow();
        result.setPublishedAt(null);
        termResultRepository.save(result);

        mockMvc.perform(post("/api/term-results/submit")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"classId":%d,"termId":%d,"sessionId":%d}
                    """.formatted(classId, termId, sessionId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SUBMITTED"));

        mockMvc.perform(post("/api/term-results/publish")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"classId":%d,"termId":%d}
                    """.formatted(classId, termId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].publishedAt").isNotEmpty());
    }

    private void seedPublishedTermResult() {
        Grade ca = new Grade();
        ca.setStudentId(studentId);
        ca.setSubjectId(subjectId);
        ca.setClassId(classId);
        ca.setTermId(termId);
        ca.setSessionId(sessionId);
        ca.setAssessmentType("CA");
        ca.setScore(new BigDecimal("40"));
        ca.setMaxScore(new BigDecimal("50"));
        ca.setTeacherId(teacherId);
        gradeRepository.save(ca);

        Grade exam = new Grade();
        exam.setStudentId(studentId);
        exam.setSubjectId(subjectId);
        exam.setClassId(classId);
        exam.setTermId(termId);
        exam.setSessionId(sessionId);
        exam.setAssessmentType("EXAM");
        exam.setScore(new BigDecimal("90"));
        exam.setMaxScore(new BigDecimal("100"));
        exam.setTeacherId(teacherId);
        gradeRepository.save(exam);

        TermResult result = new TermResult();
        result.setStudentId(studentId);
        result.setTermId(termId);
        result.setSessionId(sessionId);
        result.setClassId(classId);
        result.setAverageScore(new BigDecimal("85.00"));
        result.setRankInClass(1);
        result.setComments("Excellent progress");
        result.setPublishedAt(LocalDateTime.now());
        termResultRepository.save(result);
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
