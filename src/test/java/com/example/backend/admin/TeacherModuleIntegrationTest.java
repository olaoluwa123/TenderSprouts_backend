package com.example.backend.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.entity.Admin;
import com.example.backend.entity.ClassSubject;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Subject;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.TeacherClass;
import com.example.backend.entity.User;
import com.example.backend.repository.AdminRepository;
import com.example.backend.repository.ClassSubjectRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TeacherRepository;
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
class TeacherModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TeacherClassRepository teacherClassRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ClassSubjectRepository classSubjectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private long teacherId;
    private long classId;
    private long otherClassId;
    private long subjectId;

    @BeforeEach
    void setUp() throws Exception {
        String adminEmail = "teacher-module-admin-" + UUID.randomUUID() + "@school.local";
        User adminUser = saveUser(adminEmail, User.Role.ADMIN);
        Admin admin = new Admin();
        admin.setUserId(adminUser.getId());
        admin.setFullName("Teacher Module Admin");
        adminRepository.save(admin);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName("Module Class " + UUID.randomUUID().toString().substring(0, 8));
        schoolClass = schoolClassRepository.save(schoolClass);
        classId = schoolClass.getId();

        SchoolClass otherClass = new SchoolClass();
        otherClass.setName("Other Class " + UUID.randomUUID().toString().substring(0, 8));
        otherClass = schoolClassRepository.save(otherClass);
        otherClassId = otherClass.getId();

        Subject subject = new Subject();
        subject.setName("Mathematics " + UUID.randomUUID().toString().substring(0, 6));
        subject = subjectRepository.save(subject);
        subjectId = subject.getId();

        ClassSubject classSubject = new ClassSubject();
        classSubject.setClassId(classId);
        classSubject.setSubjectId(subjectId);
        classSubjectRepository.save(classSubject);

        User teacherUser = saveUser("teacher-module-" + UUID.randomUUID() + "@school.local", User.Role.TEACHER);
        Teacher teacher = new Teacher();
        teacher.setUserId(teacherUser.getId());
        teacher.setFullName("Module Teacher");
        teacher.setPhone("08012345678");
        teacher.setIsActive(true);
        teacher = teacherRepository.save(teacher);
        teacherId = teacher.getId();

        TeacherClass assignment = new TeacherClass();
        assignment.setTeacherId(teacherId);
        assignment.setClassId(classId);
        assignment.setStartDate(LocalDate.now());
        assignment.setIsActive(true);
        teacherClassRepository.save(assignment);

        adminToken = login(adminEmail);
    }

    @Test
    void listGetProfileUpdateAndSubjectAssign() throws Exception {
        String auth = "Bearer " + adminToken;

        mockMvc.perform(get("/api/teachers").header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[?(@.id == %d)].fullName".formatted(teacherId)).exists());

        mockMvc.perform(get("/api/teachers/" + teacherId).header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(teacherId))
            .andExpect(jsonPath("$.fullName").value("Module Teacher"))
            .andExpect(jsonPath("$.classId").value(classId));

        mockMvc.perform(get("/api/teachers/" + teacherId + "/profile")
                .header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(teacherId))
            .andExpect(jsonPath("$.classes[0].classId").value(classId))
            .andExpect(jsonPath("$.subjects").isArray());

        mockMvc.perform(patch("/api/teachers/" + teacherId)
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Updated Teacher","phone":"08099999999","isActive":true}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("Updated Teacher"))
            .andExpect(jsonPath("$.phone").value("08099999999"));

        mockMvc.perform(post("/api/teachers/" + teacherId + "/subjects")
                .param("classId", String.valueOf(classId))
                .param("subjectId", String.valueOf(subjectId))
                .header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.subjectId").value(subjectId))
            .andExpect(jsonPath("$.classId").value(classId));

        mockMvc.perform(get("/api/teachers/" + teacherId + "/subjects")
                .header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].subjectId").value(subjectId));

        mockMvc.perform(get("/api/teachers/" + teacherId + "/profile")
                .header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subjects[0].subjectId").value(subjectId));

        mockMvc.perform(post("/api/teachers/" + teacherId + "/subjects")
                .param("classId", String.valueOf(otherClassId))
                .param("subjectId", String.valueOf(subjectId))
                .header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/teachers/" + teacherId + "/subjects")
                .param("classId", String.valueOf(classId))
                .param("subjectId", String.valueOf(subjectId))
                .header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isNoContent());
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
