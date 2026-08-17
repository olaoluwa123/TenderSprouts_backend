package com.example.backend.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.entity.AcademicSession;
import com.example.backend.entity.Admin;
import com.example.backend.entity.FeeStructure;
import com.example.backend.entity.Parent;
import com.example.backend.entity.ParentStudent;
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
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.ParentStudentRepository;
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
class ParentGenerateFeesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private ParentStudentRepository parentStudentRepository;

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
    private String parentToken;
    private long sessionId;
    private long termId;
    private long classId;
    private long pupilOneId;
    private long pupilTwoId;
    private long parentId;

    @BeforeEach
    void setUp() throws Exception {
        User adminUser = saveUser("fee-gen-admin@school.local", User.Role.ADMIN);
        Admin admin = new Admin();
        admin.setUserId(adminUser.getId());
        admin.setFullName("Fee Gen Admin");
        adminRepository.save(admin);

        User parentUser = saveUser("fee-gen-parent@school.local", User.Role.PARENT);
        Parent parent = new Parent();
        parent.setUserId(parentUser.getId());
        parent.setFullName("Fee Gen Parent");
        parent = parentRepository.save(parent);
        parentId = parent.getId();

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

        pupilOneId = savePupil("First", "Child", "ADM-GEN-001", classId);
        pupilTwoId = savePupil("Second", "Child", "ADM-GEN-002", classId);
        linkParent(parentId, pupilOneId);
        linkParent(parentId, pupilTwoId);

        FeeStructure tuition = new FeeStructure();
        tuition.setName("Tuition");
        tuition.setClassId(classId);
        tuition.setSessionId(sessionId);
        tuition.setTermId(termId);
        tuition.setAmount(new BigDecimal("50000.00"));
        tuition.setIsActive(true);
        feeStructureRepository.save(tuition);

        adminToken = login("fee-gen-admin@school.local");
        parentToken = login("fee-gen-parent@school.local");
    }

    @Test
    void parentCanGenerateFeesForNewlyAddedChild() throws Exception {
        mockMvc.perform(post("/api/fees/structures/publish")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .param("classId", String.valueOf(classId))
                .param("termId", String.valueOf(termId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.invoicesCreated").value(2));

        long newPupilId = savePupil("New", "Child", "ADM-GEN-003", classId);
        linkParent(parentId, newPupilId);

        mockMvc.perform(get("/api/fees/my")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.children[?(@.studentId==" + newPupilId + ")].canGenerateFees").value(true))
            .andExpect(jsonPath("$.children[?(@.studentId==" + newPupilId + ")].missingInvoiceCount").value(1));

        mockMvc.perform(post("/api/fees/students/" + newPupilId + "/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentId").value(newPupilId))
            .andExpect(jsonPath("$.classId").value(classId))
            .andExpect(jsonPath("$.termId").value(termId))
            .andExpect(jsonPath("$.invoicesCreated").value(1));

        assertThat(invoiceRepository.findByStudentIdOrderByDueDateAscCreatedAtDesc(newPupilId)).hasSize(1);
        assertThat(invoiceRepository.findByStudentIdOrderByDueDateAscCreatedAtDesc(pupilOneId)).hasSize(1);
        assertThat(invoiceRepository.findByStudentIdOrderByDueDateAscCreatedAtDesc(pupilTwoId)).hasSize(1);

        mockMvc.perform(post("/api/fees/students/" + newPupilId + "/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.invoicesCreated").value(0));

        mockMvc.perform(get("/api/fees/my")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.children[?(@.studentId==" + newPupilId + ")].canGenerateFees").value(false));
    }

    @Test
    void parentCannotGenerateForUnlinkedPupil() throws Exception {
        mockMvc.perform(post("/api/fees/structures/publish")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .param("classId", String.valueOf(classId))
                .param("termId", String.valueOf(termId)))
            .andExpect(status().isOk());

        long unlinkedId = savePupil("Other", "Child", "ADM-GEN-999", classId);

        mockMvc.perform(post("/api/fees/students/" + unlinkedId + "/generate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
            .andExpect(status().isForbidden());
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

    private void linkParent(long parentIdValue, long studentId) {
        ParentStudent link = new ParentStudent();
        link.setParentId(parentIdValue);
        link.setStudentId(studentId);
        parentStudentRepository.save(link);
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
