package com.example.backend.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.entity.AcademicSession;
import com.example.backend.entity.Admin;
import com.example.backend.entity.FeeStructure;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Student;
import com.example.backend.entity.StudentEnrollment;
import com.example.backend.entity.Term;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.EnrollmentStatus;
import com.example.backend.repository.AcademicSessionRepository;
import com.example.backend.repository.AdminRepository;
import com.example.backend.repository.FeeStructureRepository;
import com.example.backend.repository.InvoiceRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentEnrollmentRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
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
class FeeRepublishIntegrationTest {

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
    private StudentRepository studentRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private FeeStructureRepository feeStructureRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private long sessionId;
    private long termId;
    private long classId;
    private long pupilOneId;
    private long pupilTwoId;

    @BeforeEach
    void setUp() throws Exception {
        User adminUser = saveUser("fee-republish-admin@school.local", User.Role.ADMIN);
        Admin admin = new Admin();
        admin.setUserId(adminUser.getId());
        admin.setFullName("Fee Admin");
        adminRepository.save(admin);

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
        schoolClass.setName("JSS 1");
        schoolClass = schoolClassRepository.save(schoolClass);
        classId = schoolClass.getId();

        pupilOneId = savePupil("First", "Pupil", "ADM-FEE-001", classId);
        pupilTwoId = savePupil("Second", "Pupil", "ADM-FEE-002", classId);

        FeeStructure tuition = new FeeStructure();
        tuition.setName("Tuition");
        tuition.setClassId(classId);
        tuition.setSessionId(sessionId);
        tuition.setTermId(termId);
        tuition.setAmount(new BigDecimal("50000.00"));
        tuition.setIsActive(true);
        feeStructureRepository.save(tuition);

        adminToken = login("fee-republish-admin@school.local");
    }

    @Test
    void republishCreatesInvoicesOnlyForNewPupilsWithoutPublishingDraftItems() throws Exception {
        mockMvc.perform(post("/api/fees/structures/publish")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .param("classId", String.valueOf(classId))
                .param("termId", String.valueOf(termId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.invoicesCreated").value(2))
            .andExpect(jsonPath("$.structuresPublished").value(1));

        assertThat(invoiceRepository.findByStudentIdOrderByDueDateAscCreatedAtDesc(pupilOneId)).hasSize(1);
        assertThat(invoiceRepository.findByStudentIdOrderByDueDateAscCreatedAtDesc(pupilTwoId)).hasSize(1);

        FeeStructure draftItem = new FeeStructure();
        draftItem.setName("Draft PTA");
        draftItem.setClassId(classId);
        draftItem.setSessionId(sessionId);
        draftItem.setTermId(termId);
        draftItem.setAmount(new BigDecimal("2000.00"));
        draftItem.setIsActive(true);
        draftItem = feeStructureRepository.save(draftItem);
        long draftFeeId = draftItem.getId();

        long newPupilId = savePupil("New", "Pupil", "ADM-FEE-003", classId);

        mockMvc.perform(get("/api/fees/republish-preview")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .param("classId", String.valueOf(classId))
                .param("termId", String.valueOf(termId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pupilsMissingInvoices").value(1))
            .andExpect(jsonPath("$.publishedFeeItems").value(1))
            .andExpect(jsonPath("$.pupils[0].studentId").value(newPupilId))
            .andExpect(jsonPath("$.pupils[0].missingInvoiceCount").value(1));

        mockMvc.perform(post("/api/fees/structures/republish")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .param("classId", String.valueOf(classId))
                .param("termId", String.valueOf(termId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pupilsInvoiced").value(1))
            .andExpect(jsonPath("$.invoicesCreated").value(1));

        assertThat(invoiceRepository.findByStudentIdOrderByDueDateAscCreatedAtDesc(newPupilId)).hasSize(1);
        assertThat(invoiceRepository.findByStudentIdOrderByDueDateAscCreatedAtDesc(pupilOneId)).hasSize(1);
        assertThat(invoiceRepository.findByStudentIdOrderByDueDateAscCreatedAtDesc(pupilTwoId)).hasSize(1);

        mockMvc.perform(post("/api/fees/structures/republish")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .param("classId", String.valueOf(classId))
                .param("termId", String.valueOf(termId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pupilsInvoiced").value(0))
            .andExpect(jsonPath("$.invoicesCreated").value(0));

        FeeStructure draftAfterRepublish = feeStructureRepository.findById(draftFeeId).orElseThrow();
        assertThat(draftAfterRepublish.getPublishedAt()).isNull();
        assertThat(invoiceRepository.countByFeeStructureId(draftFeeId)).isZero();
    }

    @Test
    void republishBeforeInitialPublishReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/fees/structures/republish")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .param("classId", String.valueOf(classId))
                .param("termId", String.valueOf(termId)))
            .andExpect(status().isConflict());
    }

    private long savePupil(String firstName, String lastName, String admissionNumber, long classIdValue) {
        Student student = new Student();
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setAdmissionNumber(admissionNumber);
        student.setClassId(classIdValue);
        student.setIsActive(true);
        student = studentRepository.save(student);

        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudentId(student.getId());
        enrollment.setClassId(classIdValue);
        enrollment.setSessionId(sessionId);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setStartDate(LocalDate.now());
        studentEnrollmentRepository.save(enrollment);
        return student.getId();
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
