package com.example.backend.service;

import com.example.backend.dto.preschool.PreschoolReportResponse;
import com.example.backend.dto.preschool.PublishPreschoolReportRequest;
import com.example.backend.dto.preschool.SavePreschoolReportRequest;
import com.example.backend.entity.ClassSubject;
import com.example.backend.entity.PreschoolReport;
import com.example.backend.entity.PreschoolReportComment;
import com.example.backend.entity.PreschoolReportSummary;
import com.example.backend.entity.Student;
import com.example.backend.entity.Subject;
import com.example.backend.entity.Term;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.ClassGroup;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ClassSubjectRepository;
import com.example.backend.repository.PreschoolReportCommentRepository;
import com.example.backend.repository.PreschoolReportRepository;
import com.example.backend.repository.PreschoolReportSummaryRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.security.SecurityUser;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreschoolReportService {

    private final PreschoolReportRepository preschoolReportRepository;
    private final PreschoolReportCommentRepository commentRepository;
    private final PreschoolReportSummaryRepository summaryRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final TermRepository termRepository;
    private final TeacherRepository teacherRepository;
    private final AuthorizationService authorizationService;
    private final ActiveSessionService activeSessionService;
    private final ActiveTermService activeTermService;
    private final AuditService auditService;

    public PreschoolReportService(
        PreschoolReportRepository preschoolReportRepository,
        PreschoolReportCommentRepository commentRepository,
        PreschoolReportSummaryRepository summaryRepository,
        ClassSubjectRepository classSubjectRepository,
        SubjectRepository subjectRepository,
        StudentRepository studentRepository,
        SchoolClassRepository schoolClassRepository,
        TermRepository termRepository,
        TeacherRepository teacherRepository,
        AuthorizationService authorizationService,
        ActiveSessionService activeSessionService,
        ActiveTermService activeTermService,
        AuditService auditService) {
        this.preschoolReportRepository = preschoolReportRepository;
        this.commentRepository = commentRepository;
        this.summaryRepository = summaryRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.subjectRepository = subjectRepository;
        this.studentRepository = studentRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.termRepository = termRepository;
        this.teacherRepository = teacherRepository;
        this.authorizationService = authorizationService;
        this.activeSessionService = activeSessionService;
        this.activeTermService = activeTermService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PreschoolReportResponse getSheet(Long classId, Long termId, SecurityUser actor) {
        validateClassTerm(classId, termId);
        authorizationService.requireTeacherForClass(actor, classId);
        PreschoolReport report = preschoolReportRepository.findByClassIdAndTermId(classId, termId).orElse(null);
        return toResponse(classId, termId, report);
    }

    @Transactional
    public PreschoolReportResponse saveSheet(SavePreschoolReportRequest request, SecurityUser actor) {
        validateClassTerm(request.classId(), request.termId());
        activeTermService.requireMatchesActiveTermForWrite(request.termId(), actor);
        authorizationService.requireTeacherForClass(actor, request.classId());

        Term term = termRepository.findById(request.termId())
            .orElseThrow(() -> new ResourceNotFoundException("Term not found"));
        Long sessionId = term.getSessionId() != null
            ? term.getSessionId()
            : activeSessionService.requireActiveSessionId();

        PreschoolReport report = preschoolReportRepository
            .findByClassIdAndTermId(request.classId(), request.termId())
            .orElseGet(PreschoolReport::new);
        report.setClassId(request.classId());
        report.setTermId(request.termId());
        report.setSessionId(sessionId);
        report.setTeacherId(resolveTeacherId(actor));
        report = preschoolReportRepository.save(report);

        Map<Long, String> subjectNames = classSubjectNames(request.classId());
        List<Long> currentSubjectIds = new ArrayList<>(subjectNames.keySet());

        for (SavePreschoolReportRequest.Pupil pupil : request.pupils()) {
            Student student = studentRepository.findById(pupil.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + pupil.studentId()));
            if (!request.classId().equals(student.getClassId())) {
                throw new ConflictException("Student does not belong to the selected class");
            }

            if (currentSubjectIds.isEmpty()) {
                commentRepository.findByReportIdAndStudentId(report.getId(), student.getId())
                    .forEach(commentRepository::delete);
            } else {
                commentRepository.deleteByReportIdAndStudentIdAndSubjectIdNotIn(
                    report.getId(), student.getId(), currentSubjectIds);
            }

            Map<Long, String> incoming = new HashMap<>();
            if (pupil.comments() != null) {
                for (SavePreschoolReportRequest.Comment comment : pupil.comments()) {
                    incoming.put(comment.subjectId(), comment.comment());
                }
            }
            for (Long subjectId : currentSubjectIds) {
                PreschoolReportComment row = commentRepository
                    .findByReportIdAndStudentIdAndSubjectId(report.getId(), student.getId(), subjectId)
                    .orElseGet(PreschoolReportComment::new);
                row.setReportId(report.getId());
                row.setStudentId(student.getId());
                row.setSubjectId(subjectId);
                row.setSubjectName(subjectNames.get(subjectId));
                row.setComment(blankToNull(incoming.get(subjectId)));
                commentRepository.save(row);
            }

            PreschoolReportSummary summary = summaryRepository
                .findByReportIdAndStudentId(report.getId(), student.getId())
                .orElseGet(PreschoolReportSummary::new);
            summary.setReportId(report.getId());
            summary.setStudentId(student.getId());
            summary.setClassTeacherComment(blankToNull(pupil.classTeacherComment()));
            summary.setHeadTeacherRemark(blankToNull(pupil.headTeacherRemark()));
            summaryRepository.save(summary);
        }

        auditService.logAdminAction(
            actor,
            "UPSERT",
            "preschool_reports",
            request.classId(),
            "term=" + request.termId());

        return toResponse(request.classId(), request.termId(), report);
    }

    @Transactional
    public PreschoolReportResponse publishSheet(PublishPreschoolReportRequest request, SecurityUser actor) {
        validateClassTerm(request.classId(), request.termId());
        activeTermService.requireMatchesActiveTermForWrite(request.termId(), actor);
        authorizationService.requireTeacherForClass(actor, request.classId());

        PreschoolReport report = preschoolReportRepository
            .findByClassIdAndTermId(request.classId(), request.termId())
            .orElseThrow(() -> new ConflictException("Save the preschool report before publishing"));
        if (report.getPublishedAt() == null) {
            report.setPublishedAt(LocalDateTime.now());
            preschoolReportRepository.save(report);
        }

        auditService.logAdminAction(
            actor,
            "PUBLISH",
            "preschool_reports",
            request.classId(),
            "term=" + request.termId());

        return toResponse(request.classId(), request.termId(), report);
    }

    private PreschoolReportResponse toResponse(Long classId, Long termId, PreschoolReport report) {
        List<PreschoolReportResponse.Subject> subjects = classSubjectNames(classId).entrySet().stream()
            .map(entry -> new PreschoolReportResponse.Subject(entry.getKey(), entry.getValue()))
            .toList();

        Map<Long, Map<Long, String>> commentsByStudent = new HashMap<>();
        Map<Long, PreschoolReportSummary> summaries = new HashMap<>();
        if (report != null) {
            for (PreschoolReportComment comment : commentRepository.findByReportId(report.getId())) {
                commentsByStudent
                    .computeIfAbsent(comment.getStudentId(), id -> new HashMap<>())
                    .put(comment.getSubjectId(), comment.getComment());
            }
            for (PreschoolReportSummary summary : summaryRepository.findByReportId(report.getId())) {
                summaries.put(summary.getStudentId(), summary);
            }
        }

        List<PreschoolReportResponse.Pupil> pupils = new ArrayList<>();
        for (Student student : activeStudentsInClass(classId)) {
            Map<Long, String> comments = new LinkedHashMap<>();
            Map<Long, String> stored = commentsByStudent.getOrDefault(student.getId(), Map.of());
            for (PreschoolReportResponse.Subject subject : subjects) {
                comments.put(subject.subjectId(), stored.get(subject.subjectId()));
            }
            PreschoolReportSummary summary = summaries.get(student.getId());
            pupils.add(new PreschoolReportResponse.Pupil(
                student.getId(),
                formatStudentName(student),
                student.getAdmissionNumber(),
                comments,
                summary != null ? summary.getClassTeacherComment() : null,
                summary != null ? summary.getHeadTeacherRemark() : null));
        }

        return new PreschoolReportResponse(
            report != null ? report.getId() : null,
            classId,
            termId,
            report != null ? report.getPublishedAt() : null,
            subjects,
            pupils);
    }

    private Map<Long, String> classSubjectNames(Long classId) {
        Map<Long, String> names = subjectRepository.findAll().stream()
            .collect(java.util.stream.Collectors.toMap(Subject::getId, Subject::getName, (a, b) -> a));
        Map<Long, String> ordered = new LinkedHashMap<>();
        classSubjectRepository.findByClassIdAndIsActiveTrue(classId).stream()
            .map(ClassSubject::getSubjectId)
            .distinct()
            .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(
                names.getOrDefault(a, ""), names.getOrDefault(b, "")))
            .forEach(id -> {
                String name = names.get(id);
                if (name != null && !name.isBlank()) {
                    ordered.put(id, name);
                }
            });
        return ordered;
    }

    private void validateClassTerm(Long classId, Long termId) {
        authorizationService.requireClassGroup(classId, ClassGroup.PRE_PRIMARY);
        if (!termRepository.existsById(termId)) {
            throw new ResourceNotFoundException("Term not found");
        }
    }

    private List<Student> activeStudentsInClass(Long classId) {
        return studentRepository.findByClassId(classId).stream()
            .filter(s -> !Boolean.FALSE.equals(s.getIsActive()))
            .sorted((a, b) -> formatStudentName(a).compareToIgnoreCase(formatStudentName(b)))
            .toList();
    }

    private Long resolveTeacherId(SecurityUser actor) {
        if (actor.getRole() == User.Role.ADMIN) {
            return teacherRepository.findAll().stream()
                .findFirst()
                .map(t -> t.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No teacher profile available"));
        }
        if (actor.getRole() != User.Role.TEACHER || actor.getProfileId() == null) {
            throw new ForbiddenException("Teacher profile required");
        }
        return actor.getProfileId();
    }

    private static String formatStudentName(Student student) {
        String first = student.getFirstName() != null ? student.getFirstName() : "";
        String last = student.getLastName() != null ? student.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isBlank() ? student.getAdmissionNumber() : name;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
