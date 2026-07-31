package com.example.backend.service;

import com.example.backend.dto.dashboard.ActivityItem;
import com.example.backend.dto.dashboard.AdminDashboardResponse;
import com.example.backend.dto.dashboard.ClassPerformanceItem;
import com.example.backend.dto.dashboard.GradeDistributionBucket;
import com.example.backend.dto.dashboard.KpiSummary;
import com.example.backend.dto.dashboard.PendingApprovalItem;
import com.example.backend.dto.dashboard.SubmissionFunnel;
import com.example.backend.entity.AcademicSession;
import com.example.backend.entity.AuditLog;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.Term;
import com.example.backend.entity.TermResult;
import com.example.backend.entity.TermResultSubmission;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.EnrollmentStatus;
import com.example.backend.entity.enums.TermResultSubmissionStatus;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.repository.AuditLogRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentEnrollmentRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TermResultRepository;
import com.example.backend.repository.TermResultSubmissionRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.SecurityUser;
import com.example.backend.util.LetterGradeUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardService {

    private final ActiveSessionService activeSessionService;
    private final ActiveTermService activeTermService;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final TermResultRepository termResultRepository;
    private final TermResultSubmissionRepository termResultSubmissionRepository;
    private final TermRepository termRepository;
    private final TeacherRepository teacherRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminDashboardService(
        ActiveSessionService activeSessionService,
        ActiveTermService activeTermService,
        StudentEnrollmentRepository studentEnrollmentRepository,
        UserRepository userRepository,
        SchoolClassRepository schoolClassRepository,
        TermResultRepository termResultRepository,
        TermResultSubmissionRepository termResultSubmissionRepository,
        TermRepository termRepository,
        TeacherRepository teacherRepository,
        AuditLogRepository auditLogRepository) {
        this.activeSessionService = activeSessionService;
        this.activeTermService = activeTermService;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.userRepository = userRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.termResultRepository = termResultRepository;
        this.termResultSubmissionRepository = termResultSubmissionRepository;
        this.termRepository = termRepository;
        this.teacherRepository = teacherRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public AdminDashboardResponse getDashboard(SecurityUser actor) {
        if (actor.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Admin access required");
        }

        AcademicSession session = activeSessionService.requireActiveSession();
        Term term = activeTermService.requireActiveTerm();
        Long sessionId = session.getId();
        Long termId = term.getId();

        long activeStudents = studentEnrollmentRepository.countBySessionIdAndStatus(
            sessionId, EnrollmentStatus.ACTIVE);
        long activeTeachers = userRepository.countByRoleAndIsActive(User.Role.TEACHER, true);
        long activeParents = userRepository.countByRoleAndIsActive(User.Role.PARENT, true);
        long classCount = schoolClassRepository.count();

        KpiSummary kpis = new KpiSummary(activeStudents, activeTeachers, activeParents, classCount);
        SubmissionFunnel funnel = buildFunnel(sessionId, termId, classCount);
        List<ClassPerformanceItem> classPerformance = buildClassPerformance(sessionId, termId);
        List<GradeDistributionBucket> gradeDistribution = buildGradeDistribution(termId);
        List<PendingApprovalItem> pendingApprovals = buildPendingApprovals(sessionId);
        List<ActivityItem> recentActivity = auditLogRepository.findTop15ByOrderByCreatedAtDesc().stream()
            .map(this::toActivityItem)
            .toList();

        return new AdminDashboardResponse(
            sessionId,
            session.getName(),
            termId,
            term.getName(),
            kpis,
            funnel,
            classPerformance,
            gradeDistribution,
            pendingApprovals,
            recentActivity);
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

    private List<ClassPerformanceItem> buildClassPerformance(Long sessionId, Long termId) {
        Map<Long, String> classNames = schoolClassRepository.findAll().stream()
            .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName));
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

    private List<PendingApprovalItem> buildPendingApprovals(Long sessionId) {
        return termResultSubmissionRepository
            .findBySessionIdAndStatusOrderBySubmittedAtDesc(sessionId, TermResultSubmissionStatus.SUBMITTED)
            .stream()
            .limit(10)
            .map(this::toPendingApproval)
            .toList();
    }

    private PendingApprovalItem toPendingApproval(TermResultSubmission submission) {
        String className = schoolClassRepository.findById(submission.getClassId())
            .map(SchoolClass::getName)
            .orElse("Class #" + submission.getClassId());
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
