package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.backend.config.AppFrontendProperties;
import com.example.backend.config.AppMailProperties;
import com.example.backend.dto.announcement.CreateAnnouncementRequest;
import com.example.backend.dto.announcement.UpdateAnnouncementRequest;
import com.example.backend.email.EmailJob;
import com.example.backend.email.EmailPublisher;
import com.example.backend.email.SmtpEmailSender;
import com.example.backend.entity.Announcement;
import com.example.backend.entity.Parent;
import com.example.backend.entity.ParentStudent;
import com.example.backend.entity.Student;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.AnnouncementAudience;
import com.example.backend.repository.AnnouncementRepository;
import com.example.backend.repository.AuditLogRepository;
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.SecurityUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class AnnouncementServiceEmailNotifyTest {

    private AnnouncementRepository announcementRepository;
    private SchoolClassRepository schoolClassRepository;
    private ParentRepository parentRepository;
    private ParentStudentRepository parentStudentRepository;
    private StudentRepository studentRepository;
    private TeacherRepository teacherRepository;
    private UserRepository userRepository;
    private final List<EmailJob> published = new ArrayList<>();
    private AnnouncementService service;
    private SecurityUser admin;

    @BeforeEach
    void setUp() {
        published.clear();
        announcementRepository = Mockito.mock(AnnouncementRepository.class);
        schoolClassRepository = Mockito.mock(SchoolClassRepository.class);
        parentStudentRepository = Mockito.mock(ParentStudentRepository.class);
        studentRepository = Mockito.mock(StudentRepository.class);
        parentRepository = Mockito.mock(ParentRepository.class);
        teacherRepository = Mockito.mock(TeacherRepository.class);
        userRepository = Mockito.mock(UserRepository.class);

        ParentNotifyService parentNotifyService = new ParentNotifyService(
            parentStudentRepository,
            parentRepository,
            studentRepository,
            userRepository);
        EmailService emailService = new EmailService(new CapturingEmailPublisher(published::add));
        AppFrontendProperties frontendProperties = new AppFrontendProperties();
        frontendProperties.setOrigin("https://tendersprouts.com.ng");
        AuthorizationService authorizationService = new AuthorizationService(
            Mockito.mock(TeacherClassRepository.class),
            parentStudentRepository,
            studentRepository,
            schoolClassRepository);

        service = new AnnouncementService(
            announcementRepository,
            schoolClassRepository,
            parentStudentRepository,
            studentRepository,
            parentRepository,
            teacherRepository,
            userRepository,
            parentNotifyService,
            emailService,
            frontendProperties,
            authorizationService,
            new AuditService(Mockito.mock(AuditLogRepository.class)));
        admin = new SecurityUser(1L, "admin@school.local", "x", User.Role.ADMIN, 1L);

        when(announcementRepository.save(any(Announcement.class))).thenAnswer(invocation -> {
            Announcement a = invocation.getArgument(0);
            if (a.getId() == null) {
                a.setId(99L);
            }
            return a;
        });
        when(schoolClassRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void createAllParentsSendsEmailToActiveParents() {
        Parent parent = new Parent();
        parent.setId(10L);
        parent.setUserId(20L);
        parent.setFullName("Mrs Parent");
        parent.setIsActive(true);

        User user = new User();
        user.setId(20L);
        user.setEmail("parent@school.local");
        user.setRole(User.Role.PARENT);
        user.setIsActive(true);

        when(parentRepository.findAll()).thenReturn(List.of(parent));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));

        service.create(
            new CreateAnnouncementRequest(
                AnnouncementAudience.ALL_PARENTS,
                "Holiday notice",
                "School closes Friday.",
                false,
                null,
                null,
                null,
                null),
            admin);

        assertEquals(1, published.size());
        EmailJob job = published.get(0);
        assertEquals("parent@school.local", job.to());
        assertEquals("School announcement — Holiday notice", job.subject());
        assertTrue(job.body().contains("School closes Friday."));
        assertTrue(job.body().contains("https://tendersprouts.com.ng/parent/announcements"));
    }

    @Test
    void createClassAudienceEmailsParentsOfClassPupils() {
        when(schoolClassRepository.existsById(5L)).thenReturn(true);

        Student student = new Student();
        student.setId(22L);
        student.setClassId(5L);
        when(studentRepository.findByClassId(5L, Pageable.unpaged()))
            .thenReturn(new PageImpl<>(List.of(student)));
        when(studentRepository.findById(22L)).thenReturn(Optional.of(student));

        ParentStudent link = new ParentStudent();
        link.setParentId(3L);
        link.setStudentId(22L);
        when(parentStudentRepository.findByStudentId(22L)).thenReturn(List.of(link));

        Parent parent = new Parent();
        parent.setId(3L);
        parent.setUserId(30L);
        parent.setFullName("Class Parent");
        parent.setIsActive(true);
        when(parentRepository.findById(3L)).thenReturn(Optional.of(parent));

        User user = new User();
        user.setId(30L);
        user.setEmail("class.parent@school.local");
        user.setIsActive(true);
        when(userRepository.findById(30L)).thenReturn(Optional.of(user));

        service.create(
            new CreateAnnouncementRequest(
                AnnouncementAudience.CLASS,
                "Class trip",
                "Permission slips due Monday.",
                true,
                5L,
                null,
                null,
                null),
            admin);

        assertEquals(1, published.size());
        EmailJob job = published.get(0);
        assertEquals("class.parent@school.local", job.to());
        assertEquals("School announcement — Class trip", job.subject());
        assertTrue(job.body().contains("Permission slips due Monday."));
        assertTrue(job.body().contains("https://tendersprouts.com.ng/parent/announcements"));
    }

    @Test
    void createTeachersAudienceEmailsActiveTeachers() {
        Teacher teacher = new Teacher();
        teacher.setId(7L);
        teacher.setUserId(40L);
        teacher.setFullName("Mr Teacher");
        teacher.setIsActive(true);

        User user = new User();
        user.setId(40L);
        user.setEmail("teacher@school.local");
        user.setRole(User.Role.TEACHER);
        user.setIsActive(true);

        when(teacherRepository.findAll()).thenReturn(List.of(teacher));
        when(userRepository.findById(40L)).thenReturn(Optional.of(user));

        service.create(
            new CreateAnnouncementRequest(
                AnnouncementAudience.TEACHERS,
                "Staff meeting",
                "Briefing at 3pm.",
                false,
                null,
                null,
                null,
                null),
            admin);

        assertEquals(1, published.size());
        EmailJob job = published.get(0);
        assertEquals("teacher@school.local", job.to());
        assertEquals("School announcement — Staff meeting", job.subject());
        assertTrue(job.body().contains("Briefing at 3pm."));
        assertTrue(job.body().contains("https://tendersprouts.com.ng/teacher/announcements"));
    }

    @Test
    void updateSendsUpdatedEmailToAudience() {
        Parent parent = new Parent();
        parent.setId(10L);
        parent.setUserId(20L);
        parent.setFullName("Mrs Parent");
        parent.setIsActive(true);

        User user = new User();
        user.setId(20L);
        user.setEmail("parent@school.local");
        user.setRole(User.Role.PARENT);
        user.setIsActive(true);

        Announcement existing = new Announcement();
        existing.setId(50L);
        existing.setTitle("Old");
        existing.setContent("Old content");
        existing.setAudience(AnnouncementAudience.ALL_PARENTS);
        existing.setCreatedBy(1L);
        when(announcementRepository.findById(50L)).thenReturn(Optional.of(existing));
        when(parentRepository.findAll()).thenReturn(List.of(parent));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));

        service.update(
            50L,
            new UpdateAnnouncementRequest(
                null,
                "Updated title",
                "Updated content",
                null,
                null,
                null,
                null,
                null),
            admin);

        assertEquals(1, published.size());
        EmailJob job = published.get(0);
        assertEquals("School announcement updated — Updated title", job.subject());
        assertTrue(job.body().contains("A school announcement has been updated:"));
        assertTrue(job.body().contains("Updated content"));
        assertTrue(job.body().contains("https://tendersprouts.com.ng/parent/announcements"));
    }

    private static final class CapturingEmailPublisher extends EmailPublisher {
        private final Consumer<EmailJob> sink;

        private CapturingEmailPublisher(Consumer<EmailJob> sink) {
            super(nullProvider(), new SmtpEmailSender(nullProvider(), new AppMailProperties()), null);
            this.sink = sink;
        }

        @Override
        public void publish(EmailJob job) {
            sink.accept(job);
        }

        private static <T> ObjectProvider<T> nullProvider() {
            return new ObjectProvider<>() {
                @Override
                public T getObject() throws BeansException {
                    return null;
                }

                @Override
                public T getObject(Object... args) throws BeansException {
                    return null;
                }

                @Override
                public T getIfAvailable() throws BeansException {
                    return null;
                }

                @Override
                public T getIfUnique() throws BeansException {
                    return null;
                }
            };
        }
    }
}
