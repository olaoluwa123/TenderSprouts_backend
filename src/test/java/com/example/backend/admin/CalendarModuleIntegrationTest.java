package com.example.backend.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.entity.AcademicSession;
import com.example.backend.entity.Admin;
import com.example.backend.entity.Parent;
import com.example.backend.entity.ParentStudent;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Student;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.Term;
import com.example.backend.entity.User;
import com.example.backend.repository.AcademicSessionRepository;
import com.example.backend.repository.AdminRepository;
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CalendarModuleIntegrationTest {

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
    private ParentStudentRepository parentStudentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private AcademicSessionRepository academicSessionRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String parentToken;
    private long classId;
    private long studentId;
    private long teacherId;
    private long adminUserId;

    @BeforeEach
    void setUp() throws Exception {
        User adminUser = saveUser("admin-cal@school.local", User.Role.ADMIN);
        adminUserId = adminUser.getId();
        Admin admin = new Admin();
        admin.setUserId(adminUser.getId());
        admin.setFullName("Calendar Admin");
        adminRepository.save(admin);

        User teacherUser = saveUser("teacher-cal@school.local", User.Role.TEACHER);
        Teacher teacher = new Teacher();
        teacher.setUserId(teacherUser.getId());
        teacher.setFullName("Calendar Teacher");
        teacher.setIsActive(true);
        teacher = teacherRepository.save(teacher);
        teacherId = teacher.getId();

        User parentUser = saveUser("parent-cal@school.local", User.Role.PARENT);
        Parent parent = new Parent();
        parent.setUserId(parentUser.getId());
        parent.setFullName("Calendar Parent");
        parent.setIsActive(true);
        parent = parentRepository.save(parent);

        AcademicSession session = new AcademicSession();
        session.setName("2026/2027-cal");
        session.setStartDate(LocalDate.of(2026, 9, 1));
        session.setEndDate(LocalDate.of(2027, 7, 31));
        session.setIsActive(true);
        session = academicSessionRepository.save(session);

        Term term = new Term();
        term.setSessionId(session.getId());
        term.setName("First Term");
        term.setStartDate(LocalDate.of(2026, 9, 1));
        term.setEndDate(LocalDate.of(2026, 12, 15));
        term.setIsActive(true);
        termRepository.save(term);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName("JSS 1 Cal");
        schoolClass = schoolClassRepository.save(schoolClass);
        classId = schoolClass.getId();

        Student student = new Student();
        student.setFirstName("Cal");
        student.setLastName("Pupil");
        student.setAdmissionNumber("ADM-CAL-001");
        student.setClassId(classId);
        student.setIsActive(true);
        student = studentRepository.save(student);
        studentId = student.getId();

        ParentStudent link = new ParentStudent();
        link.setParentId(parent.getId());
        link.setStudentId(studentId);
        parentStudentRepository.save(link);

        adminToken = login("admin-cal@school.local");
        parentToken = login("parent-cal@school.local");
    }

    @Test
    void calendarCrudWorks() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/calendar/events")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title":"Resumption",
                      "eventType":"RESUMPTION",
                      "startDate":"2026-09-01",
                      "endDate":"2026-09-01",
                      "classId":%d,
                      "description":"First day"
                    }
                    """.formatted(classId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Resumption"))
            .andReturn();
        long eventId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(get("/api/calendar/events")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .param("from", "2026-09-01")
                .param("to", "2026-09-30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value((int) eventId));

        mockMvc.perform(delete("/api/calendar/events/" + eventId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isNoContent());
    }

    @Test
    void announcementsCreateAndInboxForParent() throws Exception {
        mockMvc.perform(post("/api/announcements")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "audience":"ALL_PARENTS",
                      "title":"Open Day",
                      "content":"Join us on Saturday",
                      "isImportant":true
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.audience").value("ALL_PARENTS"));

        mockMvc.perform(post("/api/announcements")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "audience":"CLASS",
                      "title":"Class Note",
                      "content":"Bring PE kits",
                      "isImportant":false,
                      "classId":%d
                    }
                    """.formatted(classId)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/announcements/inbox")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.title=='Open Day')]").exists())
            .andExpect(jsonPath("$[?(@.title=='Class Note')]").exists());

        mockMvc.perform(get("/api/announcements")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void attendanceMarkAndQuery() throws Exception {
        LocalDate today = LocalDate.of(2026, 9, 10);
        mockMvc.perform(post("/api/attendance/class/mark")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "classId":%d,
                      "date":"%s",
                      "marks":[{"studentId":%d,"status":"PRESENT"}]
                    }
                    """.formatted(classId, today, studentId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].studentId").value((int) studentId))
            .andExpect(jsonPath("$[0].status").value("PRESENT"));

        mockMvc.perform(get("/api/attendance/class")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .param("classId", String.valueOf(classId))
                .param("date", today.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("PRESENT"));

        mockMvc.perform(post("/api/attendance/staff/mark")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "date":"%s",
                      "marks":[{"teacherId":%d,"status":"PRESENT"}]
                    }
                    """.formatted(today, teacherId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].teacherId").value((int) teacherId));

        mockMvc.perform(get("/api/reports/daily-attendance")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .param("date", today.toString())
                .param("classId", String.valueOf(classId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Daily Attendance"))
            .andExpect(jsonPath("$.columns").isArray());

        mockMvc.perform(get("/api/users/" + adminUserId + "/activity")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk());
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
