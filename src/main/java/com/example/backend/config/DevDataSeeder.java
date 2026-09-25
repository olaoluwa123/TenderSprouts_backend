package com.example.backend.config;

import com.example.backend.entity.AcademicSession;
import com.example.backend.entity.Admin;
import com.example.backend.entity.AdmissionApplication;
import com.example.backend.entity.Announcement;
import com.example.backend.entity.Attendance;
import com.example.backend.entity.ClassSubject;
import com.example.backend.entity.Parent;
import com.example.backend.entity.ParentStudent;
import com.example.backend.entity.SchoolCalendarEvent;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.enums.ClassGroup;
import com.example.backend.entity.Student;
import com.example.backend.entity.StudentEnrollment;
import com.example.backend.entity.Subject;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.TeacherClass;
import com.example.backend.entity.Term;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.AdmissionApplicationStatus;
import com.example.backend.entity.enums.AttendanceStatus;
import com.example.backend.entity.enums.CalendarEventType;
import com.example.backend.entity.enums.EnrollmentStatus;
import com.example.backend.entity.enums.Gender;
import com.example.backend.repository.AcademicSessionRepository;
import com.example.backend.repository.AdminRepository;
import com.example.backend.repository.AdmissionApplicationRepository;
import com.example.backend.repository.AnnouncementRepository;
import com.example.backend.repository.AttendanceRepository;
import com.example.backend.repository.ClassSubjectRepository;
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.SchoolCalendarEventRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentEnrollmentRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.AcademicSessionService;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final SeedProperties seedProperties;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final TermRepository termRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final ParentRepository parentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final AnnouncementRepository announcementRepository;
    private final SchoolCalendarEventRepository schoolCalendarEventRepository;
    private final AttendanceRepository attendanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final AcademicSessionService academicSessionService;

    public DevDataSeeder(
        SeedProperties seedProperties,
        UserRepository userRepository,
        AdminRepository adminRepository,
        TeacherRepository teacherRepository,
        AcademicSessionRepository academicSessionRepository,
        TermRepository termRepository,
        SchoolClassRepository schoolClassRepository,
        SubjectRepository subjectRepository,
        StudentRepository studentRepository,
        StudentEnrollmentRepository studentEnrollmentRepository,
        ParentRepository parentRepository,
        ParentStudentRepository parentStudentRepository,
        TeacherClassRepository teacherClassRepository,
        ClassSubjectRepository classSubjectRepository,
        AdmissionApplicationRepository admissionApplicationRepository,
        AnnouncementRepository announcementRepository,
        SchoolCalendarEventRepository schoolCalendarEventRepository,
        AttendanceRepository attendanceRepository,
        PasswordEncoder passwordEncoder,
        AcademicSessionService academicSessionService) {
        this.seedProperties = seedProperties;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.teacherRepository = teacherRepository;
        this.academicSessionRepository = academicSessionRepository;
        this.termRepository = termRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
        this.studentRepository = studentRepository;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.parentRepository = parentRepository;
        this.parentStudentRepository = parentStudentRepository;
        this.teacherClassRepository = teacherClassRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.announcementRepository = announcementRepository;
        this.schoolCalendarEventRepository = schoolCalendarEventRepository;
        this.attendanceRepository = attendanceRepository;
        this.passwordEncoder = passwordEncoder;
        this.academicSessionService = academicSessionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedProperties.isEnabled()) {
            return;
        }
        ensureStandardAcademicSessions();
        if (userRepository.findByEmail("admin@school.local").isEmpty()) {
            seedCoreAccounts();
        }
        ensureDashboardSampleData();
    }

    private void seedCoreAccounts() {
        log.info("Seeding development data");

        User adminUser = new User();
        adminUser.setEmail("admin@school.local");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setRole(User.Role.ADMIN);
        adminUser.setIsActive(true);
        adminUser = userRepository.save(adminUser);

        Admin admin = new Admin();
        admin.setUserId(adminUser.getId());
        admin.setFullName("School Admin");
        adminRepository.save(admin);

        User teacherUser = new User();
        teacherUser.setEmail("teacher@school.local");
        teacherUser.setPassword(passwordEncoder.encode("teacher123"));
        teacherUser.setRole(User.Role.TEACHER);
        teacherUser.setIsActive(true);
        teacherUser = userRepository.save(teacherUser);

        Teacher teacher = new Teacher();
        teacher.setUserId(teacherUser.getId());
        teacher.setFullName("Jane Teacher");
        teacher = teacherRepository.save(teacher);

        User parentUser = new User();
        parentUser.setEmail("parent@school.local");
        parentUser.setPassword(passwordEncoder.encode("parent123"));
        parentUser.setRole(User.Role.PARENT);
        parentUser.setIsActive(true);
        parentUser = userRepository.save(parentUser);

        Parent parent = new Parent();
        parent.setUserId(parentUser.getId());
        parent.setFullName("John Parent");
        parent.setPhone("08037696783");
        parent = parentRepository.save(parent);

        AcademicSession session = academicSessionRepository.findFirstByIsActiveTrue().orElseThrow();

        academicSessionService.ensureDefaultTermsIfMissing(session.getId());

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName("JSS 1A");
        schoolClass.setClassGroup(ClassGroup.PRIMARY);
        schoolClass = schoolClassRepository.save(schoolClass);

        TeacherClass teacherClass = new TeacherClass();
        teacherClass.setTeacherId(teacher.getId());
        teacherClass.setClassId(schoolClass.getId());
        teacherClass.setIsActive(true);
        teacherClassRepository.save(teacherClass);

        Student student = new Student();
        student.setFirstName("Ada");
        student.setLastName("Okafor");
        student.setAdmissionNumber("ADM-001");
        student.setDateOfBirth(LocalDate.of(2018, 3, 12));
        student.setGender(Gender.FEMALE);
        student.setHeight("1.20m");
        student.setWeight("24kg");
        student.setClassId(schoolClass.getId());
        student.setIsActive(true);
        student = studentRepository.save(student);

        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudentId(student.getId());
        enrollment.setClassId(schoolClass.getId());
        enrollment.setSessionId(session.getId());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setStartDate(LocalDate.now());
        enrollment = studentEnrollmentRepository.save(enrollment);
        student.setCurrentEnrollmentId(enrollment.getId());
        studentRepository.save(student);

        ParentStudent parentStudent = new ParentStudent();
        parentStudent.setParentId(parent.getId());
        parentStudent.setStudentId(student.getId());
        parentStudentRepository.save(parentStudent);

        Subject math = new Subject();
        math.setName("Mathematics");
        math = subjectRepository.save(math);

        ClassSubject classMath = new ClassSubject();
        classMath.setClassId(schoolClass.getId());
        classMath.setSubjectId(math.getId());
        classMath.setIsActive(true);
        classSubjectRepository.save(classMath);
    }

    private void ensureDashboardSampleData() {
        AcademicSession session = academicSessionRepository.findFirstByIsActiveTrue().orElse(null);
        if (session == null) {
            return;
        }
        Term term = termRepository.findFirstBySessionIdAndIsActiveTrue(session.getId()).orElse(null);
        SchoolClass schoolClass = schoolClassRepository.findAll().stream().findFirst().orElse(null);
        Long classId = schoolClass != null ? schoolClass.getId() : null;
        User adminUser = userRepository.findByEmail("admin@school.local").orElse(null);

        if (admissionApplicationRepository.count() == 0) {
            AdmissionApplication pending = new AdmissionApplication();
            pending.setApplicantFirstName("Zainab");
            pending.setApplicantLastName("Bello");
            pending.setDateOfBirth(LocalDate.of(2018, 3, 12));
            pending.setGender(Gender.FEMALE);
            pending.setProspectiveClassId(classId);
            pending.setParentFullName("Aisha Bello");
            pending.setParentEmail("aisha.bello@example.com");
            pending.setParentPhone("08030001122");
            pending.setStatus(AdmissionApplicationStatus.PENDING);
            pending.setNotes("Interested in nursery intake");
            admissionApplicationRepository.save(pending);

            AdmissionApplication pending2 = new AdmissionApplication();
            pending2.setApplicantFirstName("Ibrahim");
            pending2.setApplicantLastName("Musa");
            pending2.setDateOfBirth(LocalDate.of(2017, 11, 2));
            pending2.setGender(Gender.MALE);
            pending2.setProspectiveClassId(classId);
            pending2.setParentFullName("Fatima Musa");
            pending2.setParentEmail("fatima.musa@example.com");
            pending2.setParentPhone("08030003344");
            pending2.setStatus(AdmissionApplicationStatus.PENDING);
            admissionApplicationRepository.save(pending2);
        }

        if (announcementRepository.count() == 0 && adminUser != null) {
            Announcement one = new Announcement();
            one.setTitle("Welcome back for the new term");
            one.setContent("Classes resume on Monday. Please ensure pupils arrive by 7:45am.");
            one.setCreatedBy(adminUser.getId());
            one.setIsImportant(true);
            announcementRepository.save(one);

            Announcement two = new Announcement();
            two.setTitle("Parents' briefing");
            two.setContent("A short briefing for new parents will hold in the school hall this Friday.");
            two.setCreatedBy(adminUser.getId());
            two.setIsImportant(true);
            announcementRepository.save(two);
        }

        if (schoolCalendarEventRepository.count() == 0) {
            SchoolCalendarEvent openDay = new SchoolCalendarEvent();
            openDay.setTitle("Open Day");
            openDay.setStartDate(LocalDate.now().plusDays(10));
            openDay.setEndDate(LocalDate.now().plusDays(10));
            openDay.setEventType(CalendarEventType.GENERAL);
            openDay.setDescription("Tour of classrooms and meet the teachers.");
            schoolCalendarEventRepository.save(openDay);

            SchoolCalendarEvent midTerm = new SchoolCalendarEvent();
            midTerm.setTitle("Mid-term assessments");
            midTerm.setStartDate(LocalDate.now().plusDays(21));
            midTerm.setEndDate(LocalDate.now().plusDays(25));
            midTerm.setEventType(CalendarEventType.EXAM);
            midTerm.setDescription("Continuous assessment week.");
            schoolCalendarEventRepository.save(midTerm);

            SchoolCalendarEvent holiday = new SchoolCalendarEvent();
            holiday.setTitle("Public holiday");
            holiday.setStartDate(LocalDate.now().plusDays(35));
            holiday.setEventType(CalendarEventType.HOLIDAY);
            schoolCalendarEventRepository.save(holiday);
        }

        if (attendanceRepository.count() == 0 && classId != null) {
            Student student = studentRepository.findAll().stream().findFirst().orElse(null);
            if (student != null) {
                Attendance present = new Attendance();
                present.setStudentId(student.getId());
                present.setClassId(classId);
                present.setSessionId(session.getId());
                present.setTermId(term != null ? term.getId() : null);
                present.setDate(LocalDate.now());
                present.setStatus(AttendanceStatus.PRESENT);
                if (adminUser != null) {
                    present.setMarkedBy(adminUser.getId());
                }
                attendanceRepository.save(present);
            }
        }
    }

    private void ensureStandardAcademicSessions() {
        AcademicSession session2025 = ensureSession(
            "2025/2026",
            LocalDate.of(2025, 9, 1),
            LocalDate.of(2026, 7, 31),
            true);
        academicSessionService.ensureDefaultTermsIfMissing(session2025.getId());
        AcademicSession session2026 = ensureSession(
            "2026/2027",
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2027, 7, 31),
            false);
        academicSessionService.ensureDefaultTermsIfMissing(session2026.getId());
    }

    private AcademicSession ensureSession(
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean activeIfCreated) {
        return academicSessionRepository.findAll().stream()
            .filter(session -> name.equals(session.getName()))
            .findFirst()
            .orElseGet(() -> {
                AcademicSession session = new AcademicSession();
                session.setName(name);
                session.setStartDate(startDate);
                session.setEndDate(endDate);
                session.setIsActive(activeIfCreated);
                log.info("Ensured academic session {}", name);
                return academicSessionRepository.save(session);
            });
    }
}
