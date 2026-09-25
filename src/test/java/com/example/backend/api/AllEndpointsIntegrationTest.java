package com.example.backend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.entity.Admin;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.Term;
import com.example.backend.entity.User;
import com.example.backend.repository.AdminRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
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
class AllEndpointsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String accessToken;
    private String teacherAccessToken;
    private String refreshToken;
    private long teacherId;

    @BeforeEach
    void setUp() throws Exception {
        User adminUser = new User();
        adminUser.setEmail("api-test-admin@school.local");
        adminUser.setPassword(passwordEncoder.encode("password123"));
        adminUser.setRole(User.Role.ADMIN);
        adminUser.setIsActive(true);
        adminUser = userRepository.save(adminUser);

        Admin admin = new Admin();
        admin.setUserId(adminUser.getId());
        admin.setFullName("API Test Admin");
        adminRepository.save(admin);

        User teacherUser = new User();
        teacherUser.setEmail("api-test-teacher@school.local");
        teacherUser.setPassword(passwordEncoder.encode("password123"));
        teacherUser.setRole(User.Role.TEACHER);
        teacherUser.setIsActive(true);
        teacherUser = userRepository.save(teacherUser);

        Teacher teacher = new Teacher();
        teacher.setUserId(teacherUser.getId());
        teacher.setFullName("API Test Teacher");
        teacherId = teacherRepository.save(teacher).getId();

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"api-test-admin@school.local","password":"password123"}
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String body = login.getResponse().getContentAsString();
        accessToken = JsonPath.read(body, "$.accessToken");
        refreshToken = JsonPath.read(body, "$.refreshToken");

        MvcResult teacherLogin = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"api-test-teacher@school.local","password":"password123"}
                    """))
            .andExpect(status().isOk())
            .andReturn();
        teacherAccessToken = JsonPath.read(teacherLogin.getResponse().getContentAsString(), "$.accessToken");
    }

    @Test
    void allEndpoints_returnSuccess() throws Exception {
        String auth = "Bearer " + accessToken;
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"api-test-admin@school.local"}
                    """))
            .andExpect(status().isAccepted());

        MvcResult sessionResult = mockMvc.perform(post("/api/sessions")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"2026/2027","startDate":"2026-09-01","endDate":"2027-07-31"}
                    """))
            .andExpect(status().isCreated())
            .andReturn();
        long sessionId = readLong(sessionResult, "$.id");

        mockMvc.perform(post("/api/sessions/" + sessionId + "/activate")
                .header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk());

        long termId = termRepository.findBySessionIdOrderByNameAsc(sessionId).stream()
            .filter(t -> "First Term".equals(t.getName()))
            .findFirst()
            .orElseThrow()
            .getId();
        long secondTermId = termRepository.findBySessionIdOrderByNameAsc(sessionId).stream()
            .filter(t -> "Second Term".equals(t.getName()))
            .findFirst()
            .orElseThrow()
            .getId();

        mockMvc.perform(get("/api/sessions").header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/sessions/active").header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/terms/active").header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(termId))
            .andExpect(jsonPath("$.isActive").value(true));
        mockMvc.perform(post("/api/sessions/" + sessionId + "/terms/" + secondTermId + "/activate")
                .header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(secondTermId));
        mockMvc.perform(get("/api/terms/active").header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(secondTermId));
        mockMvc.perform(post("/api/sessions/" + sessionId + "/terms/" + termId + "/activate")
                .header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/sessions/" + sessionId + "/terms").header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].name").value("First Term"))
            .andExpect(jsonPath("$[1].name").value("Second Term"))
            .andExpect(jsonPath("$[2].name").value("Third Term"))
            .andExpect(jsonPath("$[0].id").value(termId));

        MvcResult subjectResult = mockMvc.perform(post("/api/subjects")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Math-" + suffix + "\"}"))
            .andExpect(status().isCreated())
            .andReturn();
        long subjectId = readLong(subjectResult, "$.id");

        mockMvc.perform(get("/api/subjects").header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk());

        MvcResult classResult = mockMvc.perform(post("/api/classes")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Class-" + suffix + "\",\"classGroup\":\"PRIMARY\"}"))
            .andExpect(status().isCreated())
            .andReturn();
        long classId = readLong(classResult, "$.id");

        mockMvc.perform(get("/api/classes").header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/classes/" + classId + "/subjects")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subjectId\":" + subjectId + "}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/classes/" + classId + "/subjects").header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/classes/subjects/bulk-assign")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"classIds\":[" + classId + "],\"subjectIds\":[" + subjectId + "]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.skippedCount").value(1));

        mockMvc.perform(post("/api/teachers/" + teacherId + "/classes")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .param("classId", String.valueOf(classId)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/teachers/" + teacherId + "/classes").header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk());

        MvcResult studentResult = mockMvc.perform(post("/api/students")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName":"Ada",
                      "lastName":"Lovelace",
                      "admissionNumber":"ADM-%s",
                      "classId":%d,
                      "gender":"FEMALE"
                    }
                    """.formatted(suffix, classId)))
            .andExpect(status().isCreated())
            .andReturn();
        long studentId = readLong(studentResult, "$.id");

        mockMvc.perform(get("/api/students").header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/students/" + studentId).header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/students/" + studentId)
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Ada\",\"lastName\":\"L.\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/students/" + studentId + "/enrollments").header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk());

        String gradePayload = """
            {
              "studentId":%d,
              "subjectId":%d,
              "classId":%d,
              "termId":%d,
              "assessmentType":"CA",
              "score":25,
              "maxScore":30
            }
            """.formatted(studentId, subjectId, classId, termId);

        mockMvc.perform(post("/api/grades")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(gradePayload))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/grades/batch")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"grades":[{
                      "studentId":%d,
                      "subjectId":%d,
                      "classId":%d,
                      "termId":%d,
                      "assessmentType":"EXAM",
                      "score":60,
                      "maxScore":70
                    }]}
                    """.formatted(studentId, subjectId, classId, termId)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/grades/students/" + studentId)
                .header(HttpHeaders.AUTHORIZATION, auth)
                .param("termId", String.valueOf(termId)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/grades/classes/" + classId)
                .header(HttpHeaders.AUTHORIZATION, auth)
                .param("termId", String.valueOf(termId))
                .param("subjectId", String.valueOf(subjectId)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/students/" + studentId + "/grades")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .param("termId", String.valueOf(termId)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/classes/" + classId + "/grades")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .param("termId", String.valueOf(termId))
                .param("subjectId", String.valueOf(subjectId)))
            .andExpect(status().isOk());

        MvcResult createdType = mockMvc.perform(post("/api/assessment-types")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"TESTFIELD","name":"Test Field","defaultMaxScore":10,"weight":0}
                    """))
            .andExpect(status().isCreated())
            .andReturn();
        long customTypeId = readLong(createdType, "$.id");

        mockMvc.perform(put("/api/assessment-types/" + customTypeId)
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Test Field","defaultMaxScore":10,"weight":0,"active":false}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/assessment-types").header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/term-results/compute")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"termId":%d,"classId":%d,"sessionId":%d}
                    """.formatted(termId, classId, sessionId)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/term-results/submit")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"termId":%d,"classId":%d,"sessionId":%d}
                    """.formatted(termId, classId, sessionId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SUBMITTED"));

        mockMvc.perform(get("/api/term-results/submission")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .param("classId", String.valueOf(classId))
                .param("termId", String.valueOf(termId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUBMITTED"))
            .andExpect(jsonPath("$.submittedAt").isNotEmpty());

        mockMvc.perform(get("/api/term-results/submissions")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .param("status", "SUBMITTED")
                .param("sessionId", String.valueOf(sessionId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].classId").value(classId))
            .andExpect(jsonPath("$[0].status").value("SUBMITTED"))
            .andExpect(jsonPath("$[0].resultCount").value(1));

        MvcResult termResultsPage = mockMvc.perform(get("/api/term-results")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .param("termId", String.valueOf(termId))
                .param("classId", String.valueOf(classId)))
            .andExpect(status().isOk())
            .andReturn();
        long termResultId = readLong(termResultsPage, "$.content[0].id");

        mockMvc.perform(get("/api/term-results/" + termResultId).header(HttpHeaders.AUTHORIZATION, auth))
            .andExpect(status().isOk());

        mockMvc.perform(put("/api/term-results/" + termResultId)
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comments\":\"Well done\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/term-results/approve")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"termId":%d,"classId":%d}
                    """.formatted(termId, classId)))
            .andExpect(status().isOk());

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn();
        String rotatedRefresh = JsonPath.read(refreshResult.getResponse().getContentAsString(), "$.refreshToken");

        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + rotatedRefresh + "\"}"))
            .andExpect(status().isNoContent());
    }

    private long readLong(MvcResult result, String path) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), path)).longValue();
    }

}
