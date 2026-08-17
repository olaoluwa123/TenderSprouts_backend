package com.example.backend.service;

import com.example.backend.dto.dashboard.ActivityItem;
import com.example.backend.dto.dashboard.AdminDashboardResponse;
import com.example.backend.dto.dashboard.AdmissionApplicationItem;
import com.example.backend.dto.dashboard.AnnouncementItem;
import com.example.backend.dto.dashboard.AttendanceSummary;
import com.example.backend.dto.dashboard.ClassPerformanceItem;
import com.example.backend.dto.dashboard.GradeDistributionBucket;
import com.example.backend.dto.dashboard.KpiSummary;
import com.example.backend.dto.dashboard.PendingApprovalItem;
import com.example.backend.dto.dashboard.RecentPupilItem;
import com.example.backend.dto.dashboard.SubmissionFunnel;
import com.example.backend.dto.dashboard.UpcomingEventItem;
import com.example.backend.entity.AcademicSession;
import com.example.backend.entity.AdmissionApplication;
import com.example.backend.entity.Announcement;
import com.example.backend.entity.AuditLog;
import com.example.backend.entity.SchoolCalendarEvent;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Student;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.Term;
import com.example.backend.entity.TermResult;
import com.example.backend.entity.TermResultSubmission;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.AdmissionApplicationStatus;
import com.example.backend.entity.enums.AttendanceStatus;
import com.example.backend.entity.enums.EnrollmentStatus;
import com.example.backend.entity.enums.TermResultSubmissionStatus;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.repository.AdmissionApplicationRepository;
import com.example.backend.repository.AnnouncementRepository;
import com.example.backend.repository.AttendanceRepository;
import com.example.backend.repository.AuditLogRepository;
import com.example.backend.repository.SchoolCalendarEventRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentEnrollmentRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TermResultRepository;
import com.example.backend.repository.TermResultSubmissionRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.SecurityUser;
import com.example.backend.util.LetterGradeUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardService {

    private final ActiveSessionService activeSessionService;
    private final ActiveTermService activeTermService;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final TermResultRepository termResultRepository;
    private final TermResultSubmissionRepository termResultSubmissionRepository;
    private final TermRepository termRepository;
    private final TeacherRepository teacherRepository;
    private final AuditLogRepository auditLogRepository;
    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final AnnouncementRepository announcementRepository;
    private final SchoolCalendarEventRepository schoolCalendarEventRepository;
    private final AttendanceRepository attendanceRepository;

    public AdminDashboardService(
        ActiveSessionService activeSessionService,
        ActiveTermService activeTermService,
        StudentEnrollmentRepository studentEnrollmentRepository,
        StudentRepository studentRepository,
        UserRepository userRepository,
        SchoolClassRepository schoolClassRepository,
        SubjectRepository subjectRepository,
        TermResultRepository termResultRepository,
        TermResultSubmissionRepository termResultSubmissionRepository,
        TermRepository termRepository,
        TeacherRepository teacherRepository,
        AuditLogRepository auditLogRepository,
        AdmissionApplicationRepository admissionApplicationRepository,
        AnnouncementRepository announcementRepository,
        SchoolCalendarEventRepository schoolCalendarEventRepository,
        AttendanceRepository attendanceRepository) {
        this.activeSessionService = activeSessionService;
        this.activeTermService = activeTermService;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
        this.termResultRepository = termResultRepository;
        this.termResultSubmissionRepository = termResultSubmissionRepository;
        this.termRepository = termRepository;
        this.teacherRepository = teacherRepository;
        this.auditLogRepository = auditLogRepository;
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.announcementRepository = announcementRepository;
        this.schoolCalendarEventRepository = schoolCalendarEventRepository;
        this.attendanceRepository = attendanceRepository;
    }

    public AdminDashboardResponse getDashboard(SecurityUser actor) {
        if (actor.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Admin access required");
        }

        AcademicSession session = activeSessionService.requireActiveSession();
        Term term = activeTermService.requireActiveTerm();
        Long sessionId = session.getId();
        Long termId = term.getId();

        Map<Long, String> classNames = schoolClassRepository.findAll().stream()
            .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName));

        long activeStudents = studentEnrollmentRepository.countBySessionIdAndStatus(
            sessionId, EnrollmentStatus.ACTIVE);
        long activeTeachers = userRepository.countByRoleAndIsActive(User.Role.TEACHER, true);
        long activeParents = userRepository.countByRoleAndIsActive(User.Role.PARENT, true);
        long classCount = schoolClassRepository.count();
        long subjectCount = subjectRepository.count();
        long pendingApplications = admissionApplicationRepository.countByStatus(AdmissionApplicationStatus.PENDING);

        KpiSummary kpis = new KpiSummary(
            activeStudents,
            activeTeachers,
            activeParents,
            classCount,
            subjectCount,
            pendingApplications);

        return new AdminDashboardResponse(
            sessionId,
            session.getName(),
            termId,
            term.getName(),
            kpis,
            buildFunnel(sessionId, termId, classCount),
            buildClassPerformance(sessionId, termId, classNames),
            buildGradeDistribution(termId),
            buildPendingApprovals(sessionId, classNames),
            buildNewAdmissions(classNames),
            buildRecentPupils(classNames),
            buildUpcomingEvents(classNames),
            buildAnnouncements(classNames),
            buildAttendanceSummary(sessionId, activeStudents),
            listRecentActivity(actor, 5));
    }

    public List<ActivityItem> listRecentActivity(SecurityUser actor, int limit) {
        if (actor.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Admin access required");
        }
        int capped = Math.max(1, Math.min(limit, 100));
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, capped)).stream()
            .map(this::toActivityItem)
            .toList();
    }

    private List<AdmissionApplicationItem> buildNewAdmissions(Map<Long, String> classNames) {
        return admissionApplicationRepository
            .findTop8ByStatusOrderByCreatedAtDesc(AdmissionApplicationStatus.PENDING)
            .stream()
            .map(app -> toAdmissionItem(app, classNames))
            .toList();
    }

    private AdmissionApplicationItem toAdmissionItem(AdmissionApplication app, Map<Long, String> classNames) {
        String className = app.getProspectiveClassId() != null
            ? classNames.getOrDefault(app.getProspectiveClassId(), "Class #" + app.getProspectiveClassId())
            : null;
        return new AdmissionApplicationItem(
            app.getId(),
            app.getApplicantFullName(),
            app.getParentFullName(),
            app.getParentEmail(),
            className,
            app.getStatus().name(),
            app.getCreatedAt());
    }

    private List<RecentPupilItem> buildRecentPupils(Map<Long, String> classNames) {
        return studentRepository.findTop8ByOrderByCreatedAtDesc().stream()
            .map(student -> toRecentPupil(student, classNames))
            .toList();
    }

    private RecentPupilItem toRecentPupil(Student student, Map<Long, String> classNames) {
        String className = student.getClassId() != null
            ? classNames.getOrDefault(student.getClassId(), "Class #" + student.getClassId())
            : null;
        String fullName = ((student.getFirstName() != null ? student.getFirstName() : "") + " "
            + (student.getLastName() != null ? student.getLastName() : "")).trim();
        return new RecentPupilItem(
            student.getId(),
            fullName.isBlank() ? "—" : fullName,
            student.getAdmissionNumber(),
            className,
            student.getCreatedAt());
    }

    private List<UpcomingEventItem> buildUpcomingEvents(Map<Long, String> classNames) {
        return schoolCalendarEventRepository
            .findTop6ByStartDateGreaterThanEqualOrderByStartDateAsc(LocalDate.now())
            .stream()
            .map(event -> toEventItem(event, classNames))
            .toList();
    }

    private UpcomingEventItem toEventItem(SchoolCalendarEvent event, Map<Long, String> classNames) {
        String className = event.getClassId() != null
            ? classNames.getOrDefault(event.getClassId(), "Class #" + event.getClassId())
            : "Whole school";
        return new UpcomingEventItem(
            event.getId(),
            event.getTitle(),
            event.getEventType() != null ? event.getEventType().name() : "GENERAL",
            event.getStartDate(),
            event.getEndDate(),
            className,
            event.getDescription());
    }

    private List<AnnouncementItem> buildAnnouncements(Map<Long, String> classNames) {
        List<Announcement> important = announcementRepository.findTop5ByIsImportantTrueOrderByCreatedAtDesc();
        List<Announcement> source = important.isEmpty()
            ? announcementRepository.findTop5ByOrderByCreatedAtDesc()
            : important;
        return source.stream()
            .map(announcement -> toAnnouncementItem(announcement, classNames))
            .toList();
    }

    private AnnouncementItem toAnnouncementItem(Announcement announcement, Map<Long, String> classNames) {
        String className = announcement.getClassId() != null
            ? classNames.getOrDefault(announcement.getClassId(), "Class #" + announcement.getClassId())
            : "Whole school";
        return new AnnouncementItem(
            announcement.getId(),
            announcement.getTitle(),
            announcement.getContent(),
            Boolean.TRUE.equals(announcement.getIsImportant()),
            className,
            announcement.getCreatedAt());
    }

    private AttendanceSummary buildAttendanceSummary(Long sessionId, long enrolled) {
        LocalDate date = attendanceRepository.findLatestDate();
        if (date == null) {
            date = LocalDate.now();
        }
        Map<AttendanceStatus, Long> counts = new EnumMap<>(AttendanceStatus.class);
        for (AttendanceStatus status : AttendanceStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : attendanceRepository.countByStatusForDate(date)) {
            AttendanceStatus status = (AttendanceStatus) row[0];
            long count = ((Number) row[1]).longValue();
            counts.put(status, count);
        }
        long marked = attendanceRepository.countByDate(date);
        return new AttendanceSummary(
            date,
            counts.getOrDefault(AttendanceStatus.PRESENT, 0L),
            counts.getOrDefault(AttendanceStatus.ABSENT, 0L),
            counts.getOrDefault(AttendanceStatus.LATE, 0L),
            counts.getOrDefault(AttendanceStatus.EXCUSED, 0L),
            marked,
            enrolled);
    }

    private SubmissionFunnel buildFunnel(Long sessionId, Long termId, long totalClasses) {
        List<TermResultSubmission> submissions =
            termResultSubmissionRepository.findBySessionIdAndTermId(sessionId, termId);
        Map<TermResultSubmissionStatus, Long> byStatus = new EnumMap<>(TermResultSubmissionStatus.class);
        for (TermResultSubmissionStatus status : TermResultSubmissionStatus.values()) {
            byStatus.put(status, 0L);
        }
        for (TermResultSubmission submission : submissions) {
            byStatus.merge(submission.getStatus(), 1L, Long::sum);
        }
        long draft = byStatus.getOrDefault(TermResultSubmissionStatus.DRAFT, 0L);
        long submitted = byStatus.getOrDefault(TermResultSubmissionStatus.SUBMITTED, 0L);
        long published = byStatus.getOrDefault(TermResultSubmissionStatus.PUBLISHED, 0L);
        long classesWithSubmission = draft + submitted + published;
        long notStarted = Math.max(0, totalClasses - classesWithSubmission);
        return new SubmissionFunnel(draft + notStarted, submitted, published, totalClasses);
    }

    private List<ClassPerformanceItem> buildClassPerformance(
        Long sessionId,
        Long termId,
        Map<Long, String> classNames) {
        List<Object[]> averages = termResultRepository.averageScoreByClassForTerm(termId);
        List<ClassPerformanceItem> items = new ArrayList<>();
        for (Object[] row : averages) {
            Long classId = (Long) row[0];
            BigDecimal avg = row[1] instanceof BigDecimal bd
                ? bd.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(((Number) row[1]).doubleValue()).setScale(2, RoundingMode.HALF_UP);
            long studentCount = studentEnrollmentRepository.countByClassIdAndSessionIdAndStatus(
                classId, sessionId, EnrollmentStatus.ACTIVE);
            items.add(new ClassPerformanceItem(
                classId,
                classNames.getOrDefault(classId, "Class #" + classId),
                avg,
                studentCount));
        }
        return items;
    }

    private List<GradeDistributionBucket> buildGradeDistribution(Long termId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String letter : Arrays.asList("A", "B", "C", "D", "E", "F")) {
            counts.put(letter, 0L);
        }
        for (TermResult result : termResultRepository.findByTermId(termId)) {
            String letter = LetterGradeUtil.toLetter(result.getAverageScore());
            if (letter != null) {
                counts.merge(letter, 1L, Long::sum);
            }
        }
        return counts.entrySet().stream()
            .map(e -> new GradeDistributionBucket(e.getKey(), e.getValue()))
            .toList();
    }

    private List<PendingApprovalItem> buildPendingApprovals(Long sessionId, Map<Long, String> classNames) {
        return termResultSubmissionRepository
            .findBySessionIdAndStatusOrderBySubmittedAtDesc(sessionId, TermResultSubmissionStatus.SUBMITTED)
            .stream()
            .limit(10)
            .map(submission -> toPendingApproval(submission, classNames))
            .toList();
    }

    private PendingApprovalItem toPendingApproval(TermResultSubmission submission, Map<Long, String> classNames) {
        String className = classNames.getOrDefault(submission.getClassId(), "Class #" + submission.getClassId());
        String termName = termRepository.findById(submission.getTermId())
            .map(Term::getName)
            .orElse("Term #" + submission.getTermId());
        String teacherName = submission.getSubmittedByTeacherId() != null
            ? teacherRepository.findById(submission.getSubmittedByTeacherId())
                .map(Teacher::getFullName)
                .orElse(null)
            : null;
        int resultCount = termResultRepository
            .findByClassIdAndTermIdOrderByAverageScoreDesc(submission.getClassId(), submission.getTermId())
            .size();
        return new PendingApprovalItem(
            submission.getId(),
            submission.getClassId(),
            className,
            submission.getTermId(),
            termName,
            teacherName,
            resultCount,
            submission.getSubmittedAt());
    }

    private ActivityItem toActivityItem(AuditLog log) {
        return new ActivityItem(
            log.getId(),
            log.getAction(),
            log.getEntityType(),
            log.getEntityId(),
            log.getDetails(),
            log.getUserRole(),
            log.getCreatedAt());
    }
}
