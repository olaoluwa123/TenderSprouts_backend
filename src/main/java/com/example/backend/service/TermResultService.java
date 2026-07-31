package com.example.backend.service;

import com.example.backend.dto.termresult.ComputeTermResultsRequest;
import com.example.backend.dto.termresult.PublishTermResultsRequest;
import com.example.backend.dto.termresult.SubmitTermResultsRequest;
import com.example.backend.dto.termresult.TermResultResponse;
import com.example.backend.dto.termresult.TermResultSubmissionResponse;
import com.example.backend.dto.termresult.TermResultSubmissionSummaryResponse;
import com.example.backend.dto.termresult.UpdateTermResultRequest;
import com.example.backend.entity.AssessmentTypeLookup;
import com.example.backend.entity.Grade;
import com.example.backend.entity.ParentStudent;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.StudentEnrollment;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.Term;
import com.example.backend.entity.TermResult;
import com.example.backend.entity.TermResultSubmission;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.EnrollmentStatus;
import com.example.backend.entity.enums.TermResultSubmissionStatus;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AssessmentTypeLookupRepository;
import com.example.backend.repository.GradeRepository;
import com.example.backend.config.AppFrontendProperties;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentEnrollmentRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.repository.TermResultRepository;
import com.example.backend.repository.TermResultSubmissionRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.security.SecurityUser;
import jakarta.persistence.criteria.Predicate;
import com.example.backend.util.GradeScoringUtil;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TermResultService {

    private final TermResultRepository termResultRepository;
    private final TermResultSubmissionRepository termResultSubmissionRepository;
    private final GradeRepository gradeRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final AssessmentTypeLookupRepository assessmentTypeLookupRepository;
    private final TermRepository termRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final TeacherRepository teacherRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final ParentEmailService parentEmailService;
    private final EmailService emailService;
    private final AppFrontendProperties frontendProperties;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final ActiveTermService activeTermService;

    public TermResultService(
        TermResultRepository termResultRepository,
        TermResultSubmissionRepository termResultSubmissionRepository,
        GradeRepository gradeRepository,
        StudentEnrollmentRepository studentEnrollmentRepository,
        AssessmentTypeLookupRepository assessmentTypeLookupRepository,
        TermRepository termRepository,
        SchoolClassRepository schoolClassRepository,
        TeacherClassRepository teacherClassRepository,
        TeacherRepository teacherRepository,
        ParentStudentRepository parentStudentRepository,
        ParentEmailService parentEmailService,
        EmailService emailService,
        AppFrontendProperties frontendProperties,
        AuthorizationService authorizationService,
        AuditService auditService,
        ActiveTermService activeTermService) {
        this.termResultRepository = termResultRepository;
        this.termResultSubmissionRepository = termResultSubmissionRepository;
        this.gradeRepository = gradeRepository;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.assessmentTypeLookupRepository = assessmentTypeLookupRepository;
        this.termRepository = termRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.teacherClassRepository = teacherClassRepository;
        this.teacherRepository = teacherRepository;
        this.parentStudentRepository = parentStudentRepository;
        this.parentEmailService = parentEmailService;
        this.emailService = emailService;
        this.frontendProperties = frontendProperties;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.activeTermService = activeTermService;
    }

    public Page<TermResultResponse> list(
        Long termId,
        Long classId,
        Long studentId,
        SecurityUser actor,
        Pageable pageable) {
        Specification<TermResult> spec = buildListSpec(termId, classId, studentId, actor);
        return termResultRepository.findAll(spec, pageable).map(TermResultResponse::from);
    }

    public TermResultSubmissionResponse getSubmission(Long classId, Long termId, SecurityUser actor) {
        TermResultSubmission submission = termResultSubmissionRepository.findByClassIdAndTermId(classId, termId)
            .orElseThrow(() -> new ResourceNotFoundException("No submission found for this class and term"));
        assertCanViewSubmission(submission, actor);
        return TermResultSubmissionResponse.from(submission);
    }

    public List<TermResultSubmissionSummaryResponse> listSubmissions(
        TermResultSubmissionStatus status,
        Long sessionId,
        SecurityUser actor) {
        if (actor.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Only admins can list term result submissions");
        }
        List<TermResultSubmission> submissions = sessionId != null && status != null
            ? termResultSubmissionRepository.findBySessionIdAndStatusOrderBySubmittedAtDesc(sessionId, status)
            : status != null
                ? termResultSubmissionRepository.findByStatusOrderBySubmittedAtDesc(status)
                : termResultSubmissionRepository.findAll().stream()
                    .sorted((a, b) -> {
                        LocalDateTime left = a.getSubmittedAt() != null ? a.getSubmittedAt() : a.getCreatedAt();
                        LocalDateTime right = b.getSubmittedAt() != null ? b.getSubmittedAt() : b.getCreatedAt();
                        if (left == null && right == null) {
                            return 0;
                        }
                        if (left == null) {
                            return 1;
                        }
                        if (right == null) {
                            return -1;
                        }
                        return right.compareTo(left);
                    })
                    .toList();

        return submissions.stream()
            .map(this::toSubmissionSummary)
            .toList();
    }

    private TermResultSubmissionSummaryResponse toSubmissionSummary(TermResultSubmission submission) {
        String className = schoolClassRepository.findById(submission.getClassId())
            .map(SchoolClass::getName)
            .orElse(null);
        String termName = termRepository.findById(submission.getTermId())
            .map(Term::getName)
            .orElse(null);
        String teacherName = submission.getSubmittedByTeacherId() != null
            ? teacherRepository.findById(submission.getSubmittedByTeacherId())
                .map(Teacher::getFullName)
                .orElse(null)
            : null;
        int resultCount = termResultRepository
            .findByClassIdAndTermIdOrderByAverageScoreDesc(submission.getClassId(), submission.getTermId())
            .size();
        return TermResultSubmissionSummaryResponse.from(
            submission, className, termName, teacherName, resultCount);
    }

    public TermResultResponse getById(Long id, SecurityUser actor) {
        TermResult result = findResult(id);
        assertCanView(result, actor);
        return TermResultResponse.from(result);
    }

    @Transactional
    public List<TermResultResponse> compute(ComputeTermResultsRequest request, SecurityUser actor) {
        if (actor.getRole() == User.Role.TEACHER) {
            authorizationService.requireTeacherForClass(actor, request.classId());
        } else if (actor.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Not allowed to compute term results");
        }

        termRepository.findById(request.termId())
            .orElseThrow(() -> new ResourceNotFoundException("Term not found"));
        activeTermService.requireMatchesActiveTermForWrite(request.termId(), actor);
        schoolClassRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        TermResultSubmission submission = getOrCreateSubmission(
            request.classId(), request.termId(), request.sessionId());
        if (submission.getStatus() == TermResultSubmissionStatus.PUBLISHED) {
            throw new ConflictException("Results are already published");
        }
        if (submission.getStatus() == TermResultSubmissionStatus.SUBMITTED) {
            throw new ConflictException("Results already submitted to admin for review");
        }

        List<AssessmentTypeLookup> weightedTypes = assessmentTypeLookupRepository
            .findByIsActiveTrueOrderBySortOrderAscCodeAsc().stream()
            .filter(type -> type.getWeight() != null && type.getWeight().compareTo(BigDecimal.ZERO) > 0)
            .toList();
        Set<String> weightedTypeCodes = GradeScoringUtil.weightedTypeCodes(weightedTypes);
        List<Grade> grades = gradeRepository.findByClassIdAndTermId(request.classId(), request.termId());
        Map<Long, List<Grade>> gradesByStudent = grades.stream()
            .collect(Collectors.groupingBy(Grade::getStudentId));

        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByClassIdAndSessionIdAndStatus(
            request.classId(), request.sessionId(), EnrollmentStatus.ACTIVE);
        if (enrollments.isEmpty()) {
            throw new ConflictException(
                "No active student enrollments for this class in the selected session. Enroll students before computing results.");
        }

        for (StudentEnrollment enrollment : enrollments) {
            Long studentId = enrollment.getStudentId();
            BigDecimal average = GradeScoringUtil.computeTermAverage(
                gradesByStudent.getOrDefault(studentId, List.of()), weightedTypeCodes);
            TermResult result = termResultRepository.findByStudentIdAndTermId(studentId, request.termId())
                .orElseGet(TermResult::new);
            result.setStudentId(studentId);
            result.setTermId(request.termId());
            result.setSessionId(request.sessionId());
            result.setClassId(request.classId());
            result.setAverageScore(average);
            result.setPublishedAt(null);
            termResultRepository.save(result);
        }

        submission.setStatus(TermResultSubmissionStatus.DRAFT);
        submission.setSubmittedAt(null);
        submission.setSubmittedByTeacherId(null);
        submission.setPublishedAt(null);
        termResultSubmissionRepository.save(submission);

        recomputeRanks(request.classId(), request.termId());
        auditService.logAdminAction(
            actor,
            "COMPUTE",
            "term_results",
            request.classId(),
            "termId=" + request.termId());

        return termResultRepository.findByClassIdAndTermIdOrderByAverageScoreDesc(
                request.classId(), request.termId()).stream()
            .map(TermResultResponse::from)
            .toList();
    }

    @Transactional
    public TermResultSubmissionResponse submit(SubmitTermResultsRequest request, SecurityUser actor) {
        if (actor.getRole() != User.Role.TEACHER || actor.getProfileId() == null) {
            throw new ForbiddenException("Only teachers can submit results to admin");
        }
        authorizationService.requireTeacherForClass(actor, request.classId());
        activeTermService.requireMatchesActiveTermForWrite(request.termId(), actor);

        List<TermResult> results = termResultRepository.findByClassIdAndTermIdOrderByAverageScoreDesc(
            request.classId(), request.termId());
        if (results.isEmpty()) {
            throw new ResourceNotFoundException("Compute results before submitting to admin");
        }

        TermResultSubmission submission = getOrCreateSubmission(
            request.classId(), request.termId(), request.sessionId());
        if (submission.getStatus() == TermResultSubmissionStatus.PUBLISHED) {
            throw new ConflictException("Results are already published");
        }
        if (submission.getStatus() == TermResultSubmissionStatus.SUBMITTED) {
            throw new ConflictException("Results already submitted to admin");
        }

        submission.setStatus(TermResultSubmissionStatus.SUBMITTED);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setSubmittedByTeacherId(actor.getProfileId());
        submission = termResultSubmissionRepository.save(submission);

        auditService.logAdminAction(
            actor,
            "SUBMIT",
            "term_result_submission",
            submission.getId(),
            "classId=" + request.classId() + ",termId=" + request.termId());

        return TermResultSubmissionResponse.from(submission);
    }

    @Transactional
    public List<TermResultResponse> publish(PublishTermResultsRequest request, SecurityUser actor) {
        if (actor.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Only admins can publish results for parents");
        }

        TermResultSubmission submission = termResultSubmissionRepository
            .findByClassIdAndTermId(request.classId(), request.termId())
            .orElseThrow(() -> new ResourceNotFoundException("No submitted results found for this class and term"));
        if (submission.getStatus() != TermResultSubmissionStatus.SUBMITTED) {
            throw new ConflictException("Results must be submitted by a teacher before publishing");
        }

        List<TermResult> results = termResultRepository.findByClassIdAndTermIdOrderByAverageScoreDesc(
            request.classId(), request.termId());
        if (results.isEmpty()) {
            throw new ResourceNotFoundException("No term results found for this class and term");
        }

        LocalDateTime now = LocalDateTime.now();
        Set<Long> publishedStudentIds = new HashSet<>();
        for (TermResult result : results) {
            if (result.getPublishedAt() == null) {
                result.setPublishedAt(now);
                termResultRepository.save(result);
                publishedStudentIds.add(result.getStudentId());
            }
        }

        submission.setStatus(TermResultSubmissionStatus.PUBLISHED);
        submission.setPublishedAt(now);
        termResultSubmissionRepository.save(submission);

        if (!publishedStudentIds.isEmpty()) {
            String portalUrl = frontendProperties.getOrigin().replaceAll("/$", "") + "/results";
            parentEmailService.emailParentsForStudents(
                publishedStudentIds,
                (user, parent) -> emailService.sendTermResultsPublished(
                    user.getEmail(),
                    parent.getFullName(),
                    String.join(", ", parentEmailService.childNamesForParent(parent.getId())),
                    portalUrl));
        }

        auditService.logAdminAction(
            actor,
            "PUBLISH",
            "term_results",
            request.classId(),
            "termId=" + request.termId());

        return results.stream().map(TermResultResponse::from).toList();
    }

    @Transactional
    public TermResultResponse update(Long id, UpdateTermResultRequest request, SecurityUser actor) {
        if (actor.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Only admins can edit results during review");
        }
        TermResult result = findResult(id);
        TermResultSubmission submission = termResultSubmissionRepository
            .findByClassIdAndTermId(result.getClassId(), result.getTermId())
            .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
        if (submission.getStatus() != TermResultSubmissionStatus.SUBMITTED) {
            throw new ConflictException("Results can only be edited while awaiting admin review");
        }

        if (request.averageScore() != null) {
            result.setAverageScore(request.averageScore());
        }
        if (request.comments() != null) {
            result.setComments(request.comments());
        }
        result = termResultRepository.save(result);
        recomputeRanks(result.getClassId(), result.getTermId());
        auditService.logAdminAction(actor, "UPDATE", "term_result", result.getId(), null);
        return TermResultResponse.from(result);
    }

    private TermResultSubmission getOrCreateSubmission(Long classId, Long termId, Long sessionId) {
        return termResultSubmissionRepository.findByClassIdAndTermId(classId, termId)
            .orElseGet(() -> {
                TermResultSubmission created = new TermResultSubmission();
                created.setClassId(classId);
                created.setTermId(termId);
                created.setSessionId(sessionId);
                created.setStatus(TermResultSubmissionStatus.DRAFT);
                return termResultSubmissionRepository.save(created);
            });
    }

    private void recomputeRanks(Long classId, Long termId) {
        List<TermResult> results = termResultRepository.findByClassIdAndTermIdOrderByAverageScoreDesc(classId, termId);
        int rank = 0;
        int position = 0;
        BigDecimal previousScore = null;
        for (TermResult result : results) {
            position++;
            BigDecimal score = result.getAverageScore();
            if (previousScore == null || score == null || score.compareTo(previousScore) != 0) {
                rank = position;
                previousScore = score;
            }
            result.setRankInClass(rank);
            termResultRepository.save(result);
        }
    }

    private Specification<TermResult> buildListSpec(
        Long termId,
        Long classId,
        Long studentId,
        SecurityUser actor) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (termId != null) {
                predicates.add(cb.equal(root.get("termId"), termId));
            }
            if (classId != null) {
                predicates.add(cb.equal(root.get("classId"), classId));
            }
            if (studentId != null) {
                predicates.add(cb.equal(root.get("studentId"), studentId));
            }

            if (actor.getRole() == User.Role.ADMIN || actor.getRole() == User.Role.TEACHER) {
                if (actor.getRole() == User.Role.TEACHER) {
                    Set<Long> classIds = teacherClassRepository.findByTeacherId(actor.getProfileId()).stream()
                        .filter(tc -> Boolean.TRUE.equals(tc.getIsActive()))
                        .map(tc -> tc.getClassId())
                        .collect(Collectors.toSet());
                    if (classIds.isEmpty()) {
                        predicates.add(cb.disjunction());
                    } else {
                        predicates.add(root.get("classId").in(classIds));
                    }
                }
                return cb.and(predicates.toArray(Predicate[]::new));
            }

            if (actor.getRole() == User.Role.PARENT) {
                predicates.add(cb.isNotNull(root.get("publishedAt")));
                Set<Long> studentIds = parentStudentRepository.findByParentId(actor.getProfileId()).stream()
                    .map(ParentStudent::getStudentId)
                    .collect(Collectors.toSet());
                if (studentIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("studentId").in(studentIds));
                }
                return cb.and(predicates.toArray(Predicate[]::new));
            }

            predicates.add(cb.disjunction());
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void assertCanViewSubmission(TermResultSubmission submission, SecurityUser actor) {
        if (actor.getRole() == User.Role.ADMIN) {
            return;
        }
        if (actor.getRole() == User.Role.TEACHER) {
            authorizationService.requireTeacherForClass(actor, submission.getClassId());
            return;
        }
        throw new ForbiddenException("Not allowed to view this submission");
    }

    private void assertCanView(TermResult result, SecurityUser actor) {
        if (actor.getRole() == User.Role.ADMIN) {
            return;
        }
        if (actor.getRole() == User.Role.TEACHER) {
            authorizationService.requireTeacherForClass(actor, result.getClassId());
            return;
        }
        if (actor.getRole() == User.Role.PARENT) {
            if (result.getPublishedAt() == null) {
                throw new ForbiddenException("Term result not published");
            }
            if (!parentStudentRepository.existsByParentIdAndStudentId(actor.getProfileId(), result.getStudentId())) {
                throw new ForbiddenException("Not allowed to view this term result");
            }
            return;
        }
        throw new ForbiddenException("Not allowed to view this term result");
    }

    private TermResult findResult(Long id) {
        return termResultRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Term result not found"));
    }
}
