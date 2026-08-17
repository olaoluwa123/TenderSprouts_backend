package com.example.backend.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.entity.AcademicSession;
import com.example.backend.entity.Admin;
import com.example.backend.entity.Attendance;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Student;
import com.example.backend.entity.Subject;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.TeacherClass;
import com.example.backend.entity.Term;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.AttendanceStatus;
import com.example.backend.repository.AcademicSessionRepository;
import com.example.backend.repository.AdminRepository;
import com.example.backend.repository.AttendanceRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.repository.UserRepository;
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
class ClassSubjectModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TeacherClassRepository teacherClassRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private AcademicSessionRepository academicSessionRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private long classId;
    private long subjectId;
    private long teacherId;

    @BeforeEach
    void setUp() throws Exception {
        String adminEmail = "class-subj-admin-" + UUID.randomUUID() + "@school.local";
        User adminUser = saveUser(adminEmail, User.Role.ADMIN);
        Admin admin = new Admin();
        admin.setUserId(adminUser.getId());
        admin.setFullName("Class Subject Admin");
        adminRepository.save(admin);

        AcademicSession session = new AcademicSession();
        session.setName("CS" + UUID.randomUUID().toString().substring(0, 8));
        session.setStartDate(LocalDate.of(2026, 9, 1));
        session.setEndDate(LocalDate.of(2027, 7, 31));
        session.setIsActive(true);
        session = academicSessionRepository.save(session);

        Term term = new Term();
        term.setSessionId(session.getId());
        term.setName("First Term");
        term.setIsActive(true);
        termRepository.save(term);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName("CS Class " + UUID.randomUUID().toString().substring(0, 6));
        schoolClass = schoolClassRepository.save(schoolClass);
        classId = schoolClass.getId();

        Subject subject = new Subject();
        subject.setName("CS Subject " + UUID.randomUUID().toString().substring(0, 6));
        subject.setIsActive(true);
        subject = subjectRepository.save(subject);
        subjectId = subject.getId();

        User teacherUser = saveUser("cs-teacher-" + UUID.randomUUID() + "@school.local", User.Role.TEACHER);
        Teacher teacher = new Teacher();
        teacher.setUserId(teacherUser.getId());
        teacher.setFullName("CS Teacher");
        teacher.setIsActive(true);
        teacher = teacherRepository.save(teacher);
        teacherId = teacher.getId();

        TeacherClass assignment = new TeacherClass();
        assignment.setTeacherId(teacherId);
        assignment.setClassId(classId);
        assignment.setStartDate(LocalDate.now());
        assignment.setIsActive(true);
        teacherClassRepository.save(assignment);

        Student student = new Student();
        student.setFirstName("Ada");
        student.setLastName("Pupil");
        student.setAdmissionNumber("ADM-CS-" + UUID.randomUUID().toString().substring(0, 6));
        student.setClassId(classId);
        student.setIsActive(true);
        student = studentRepository.save(student);

        Attendance attendance = new Attendance();
        attendance.setStudentId(student.getId());
        attendance.setClassId(classId);
        attendance.setDate(LocalDate.now());
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendanceRepository.save(attendance);

        adminToken = login(adminEmail);
    }

    @Test
    void classProfileSubjectCrudAndUnassign() throws Exception {
        String auth = "Bearer " + adminToken;

        mockMvc.perform(get("/api/classes").header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[?(@.id == %d)].teacherName".formatted(classId)).value("CS Teacher"))
            .andExpect(jsonPath("$.content[?(@.id == %d)].pupilCount".formatted(classId)).value(1));

        mockMvc.perform(post("/api/classes/" + classId + "/subjects")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subjectId\":%d}".formatted(subjectId)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/classes/" + classId).header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(classId))
            .andExpect(jsonPath("$.teacher.fullName").value("CS Teacher"))
            .andExpect(jsonPath("$.pupilCount").value(1))
            .andExpect(jsonPath("$.subjects[0].subjectId").value(subjectId))
            .andExpect(jsonPath("$.attendance.present").value(1));

        mockMvc.perform(get("/api/subjects/" + subjectId).header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.classes[0].classId").value(classId));

        mockMvc.perform(patch("/api/subjects/" + subjectId)
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Renamed Subject\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Renamed Subject"));

        mockMvc.perform(delete("/api/classes/" + classId + "/subjects/" + subjectId)
                .header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/classes/" + classId + "/subjects")
                .header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(delete("/api/subjects/" + subjectId).header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/subjects/" + subjectId).header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isActive").value(false));
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
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"password123"}
                    """.formatted(email)))
            .andExpect(status().isOk())
            .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
