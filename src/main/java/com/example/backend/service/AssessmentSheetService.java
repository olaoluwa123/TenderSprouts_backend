package com.example.backend.service;

import com.example.backend.dto.assessment.AssessmentSheetResponse;
import com.example.backend.dto.assessment.MidtermSubmissionSummaryResponse;
import com.example.backend.dto.assessment.PublishAssessmentSheetRequest;
import com.example.backend.dto.assessment.SaveAssessmentSheetRequest;
import com.example.backend.dto.parentreport.StudentAssessmentReportResponse;
import com.example.backend.entity.AssessmentSheet;
import com.example.backend.entity.AssessmentSheetCell;
import com.example.backend.entity.AssessmentSheetPublication;
import com.example.backend.entity.AssessmentSheetColumn;
import com.example.backend.entity.AssessmentSheetScoreType;
import com.example.backend.entity.ClassSubject;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Teacher;
import com.example.backend.config.AppFrontendProperties;
import com.example.backend.email.EmailAttachment;
import com.example.backend.entity.Student;
import com.example.backend.entity.Subject;
import com.example.backend.entity.Term;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.AssessmentSheetType;
import com.example.backend.entity.enums.ClassGroup;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AssessmentSheetCellRepository;
import com.example.backend.repository.AssessmentSheetColumnRepository;
import com.example.backend.repository.AssessmentSheetPublicationRepository;
import com.example.backend.repository.AssessmentSheetRepository;
import com.example.backend.repository.AssessmentSheetScoreTypeRepository;
import com.example.backend.repository.ClassSubjectRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.security.SecurityUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssessmentSheetService {

    private static final Logger log = LoggerFactory.getLogger(AssessmentSheetService.class);
    private static final BigDecimal DEFAULT_MAX = new BigDecimal("100");
    private static final String PROJECT_TYPE = "PROJECT";

    private final AssessmentSheetRepository assessmentSheetRepository;
    private final AssessmentSheetColumnRepository columnRepository;
    private final AssessmentSheetCellRepository cellRepository;
    private final AssessmentSheetPublicationRepository publicationRepository;
    private final AssessmentSheetScoreTypeRepository scoreTypeRepository;
    private final StudentRepository studentRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final TermRepository termRepository;
    private final TeacherRepository teacherRepository;
    private final AuthorizationService authorizationService;
    private final ActiveSessionService activeSessionService;
    private final ActiveTermService activeTermService;
    private final AuditService auditService;
    private final ParentPublishedReportService parentPublishedReportService;
    private final ParentNotifyService parentNotifyService;
    private final EmailService emailService;
    private final AppFrontendProperties frontendProperties;

    public AssessmentSheetService(
        AssessmentSheetRepository assessmentSheetRepository,
        AssessmentSheetColumnRepository columnRepository,
        AssessmentSheetCellRepository cellRepository,
        AssessmentSheetPublicationRepository publicationRepository,
        AssessmentSheetScoreTypeRepository scoreTypeRepository,
        StudentRepository studentRepository,
        ClassSubjectRepository classSubjectRepository,
        SubjectRepository subjectRepository,
        SchoolClassRepository schoolClassRepository,
        TermRepository termRepository,
        TeacherRepository teacherRepository,
        AuthorizationService authorizationService,
        ActiveSessionService activeSessionService,
        ActiveTermService activeTermService,
        AuditService auditService,
        ParentPublishedReportService parentPublishedReportService,
        ParentNotifyService parentNotifyService,
        EmailService emailService,
        AppFrontendProperties frontendProperties) {
        this.assessmentSheetRepository = assessmentSheetRepository;
        this.columnRepository = columnRepository;
        this.cellRepository = cellRepository;
        this.publicationRepository = publicationRepository;
        this.scoreTypeRepository = scoreTypeRepository;
        this.studentRepository = studentRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.subjectRepository = subjectRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.termRepository = termRepository;
        this.teacherRepository = teacherRepository;
        this.authorizationService = authorizationService;
        this.activeSessionService = activeSessionService;
        this.activeTermService = activeTermService;
        this.auditService = auditService;
        this.parentPublishedReportService = parentPublishedReportService;
        this.parentNotifyService = parentNotifyService;
        this.emailService = emailService;
        this.frontendProperties = frontendProperties;
    }

    @Transactional
    public AssessmentSheetResponse getSheet(
        AssessmentSheetType type,
        Long classId,
        Long termId,
        Integer weekNumber,
        SecurityUser actor) {
        int week = normalizeWeek(type, weekNumber);
        validateClassTerm(classId, termId);
        authorizationService.requireTeacherForClass(actor, classId);

        AssessmentSheet sheet = assessmentSheetRepository
            .findBySheetTypeAndClassIdAndTermIdAndWeekNumber(type, classId, termId, week)
            .orElse(null);
        if (sheet != null) {
            stripProjectFromMidterm(sheet);
            ensureClassSubjectsOnUnpublishedSheet(sheet);
        }
        return buildResponse(type, classId, termId, week, sheet);
    }

    @Transactional(readOnly = true)
    public StudentAssessmentReportResponse preview(
        AssessmentSheetType type,
        Long classId,
        Long termId,
        Integer weekNumber,
        Long studentId,
        SecurityUser actor) {
        AssessmentSheet sheet = requireSavedSheet(type, classId, termId, weekNumber, studentId, actor);
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        return parentPublishedReportService.previewAssessment(student, sheet);
    }

    @Transactional(readOnly = true)
    public byte[] previewPdf(
        AssessmentSheetType type,
        Long classId,
        Long termId,
        Integer weekNumber,
        Long studentId,
        SecurityUser actor) {
        return parentPublishedReportService.previewAssessmentPdf(
            preview(type, classId, termId, weekNumber, studentId, actor));
    }

    private AssessmentSheet requireSavedSheet(
        AssessmentSheetType type,
        Long classId,
        Long termId,
        Integer weekNumber,
        Long studentId,
        SecurityUser actor) {
        int week = normalizeWeek(type, weekNumber);
        validateClassTerm(classId, termId);
        authorizationService.requireTeacherForClass(actor, classId);
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (!classId.equals(student.getClassId())) {
            throw new ConflictException("Student does not belong to the selected class");
        }
        return assessmentSheetRepository
            .findBySheetTypeAndClassIdAndTermIdAndWeekNumber(type, classId, termId, week)
            .orElseThrow(() -> new ConflictException("Save the sheet before viewing the report"));
    }

    @Transactional
    public AssessmentSheetResponse saveSheet(SaveAssessmentSheetRequest request, SecurityUser actor) {
        AssessmentSheetType type = request.type();
        int week = normalizeWeek(type, request.weekNumber());
        validateClassTerm(request.classId(), request.termId());
        activeTermService.requireMatchesActiveTermForWrite(request.termId(), actor);
        authorizationService.requireTeacherForClass(actor, request.classId());

        Long teacherId = resolveTeacherId(actor);
        Long sessionId = activeSessionService.requireActiveSessionId();

        Set<String> clientKeys = new HashSet<>();
        for (SaveAssessmentSheetRequest.ColumnInput col : request.columns()) {
            if (!clientKeys.add(col.clientKey())) {
                throw new ConflictException("Duplicate column clientKey: " + col.clientKey());
            }
            if (col.name() == null || col.name().isBlank()) {
                throw new ConflictException("Column name is required");
            }
        }

        AssessmentSheet sheet = assessmentSheetRepository
            .findBySheetTypeAndClassIdAndTermIdAndWeekNumber(
                type, request.classId(), request.termId(), week)
            .orElseGet(AssessmentSheet::new);
        requireMidtermEditable(type, sheet);
        sheet.setSheetType(type);
        sheet.setClassId(request.classId());
        sheet.setTermId(request.termId());
        sheet.setWeekNumber(week);
        sheet.setSessionId(sessionId);
        sheet.setTeacherId(teacherId);
        sheet = assessmentSheetRepository.save(sheet);
        stripProjectFromMidterm(sheet);

        List<SaveAssessmentSheetRequest.TypeInput> incomingTypes =
            request.types() == null ? List.of() : request.types();
        Map<String, AssessmentSheetScoreType> typesByCode = upsertScoreTypes(
            sheet,
            type == AssessmentSheetType.MIDTERM
                ? incomingTypes.stream().filter(t -> !isProjectType(t.code())).toList()
                : incomingTypes);

        List<AssessmentSheetColumn> existingColumns =
            columnRepository.findBySheetIdOrderBySortOrderAscIdAsc(sheet.getId());
        Map<Long, AssessmentSheetColumn> existingById = existingColumns.stream()
            .collect(Collectors.toMap(AssessmentSheetColumn::getId, c -> c, (a, b) -> a));

        Set<Long> keepIds = request.columns().stream()
            .map(SaveAssessmentSheetRequest.ColumnInput::id)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
        Set<String> requiredSubjects = new HashSet<>();
        if (sheet.getPublishedAt() == null && Objects.equals(request.termId(), currentActiveTermId())) {
            for (AssessmentSheetResponse.Column seeded : seedSubjectColumns(request.classId())) {
                if (seeded.name() != null) {
                    requiredSubjects.add(seeded.name().trim().toLowerCase());
                }
            }
        }

        for (AssessmentSheetColumn existing : existingColumns) {
            if (keepIds.contains(existing.getId())) {
                continue;
            }
            String existingName = existing.getName() != null ? existing.getName().trim().toLowerCase() : "";
            if (requiredSubjects.contains(existingName)) {
                continue;
            }
            cellRepository.deleteByColumnId(existing.getId());
            columnRepository.delete(existing);
        }

        Map<String, AssessmentSheetColumn> byClientKey = new LinkedHashMap<>();
        int order = 0;
        for (SaveAssessmentSheetRequest.ColumnInput input : request.columns()) {
            AssessmentSheetColumn column;
            if (input.id() != null) {
                column = existingById.get(input.id());
                if (column == null || !sheet.getId().equals(column.getSheetId())) {
                    throw new ConflictException("Unknown column id: " + input.id());
                }
            } else {
                column = new AssessmentSheetColumn();
                column.setSheetId(sheet.getId());
            }
            column.setName(input.name().trim());
            column.setSortOrder(input.sortOrder() != null ? input.sortOrder() : order);
            column.setMaxScore(input.maxScore() != null ? input.maxScore() : DEFAULT_MAX);
            column = columnRepository.save(column);
            byClientKey.put(input.clientKey(), column);
            order++;
        }

        Set<Long> activeStudentIds = activeStudentsInClass(request.classId()).stream()
            .map(Student::getId)
            .collect(Collectors.toSet());

        for (SaveAssessmentSheetRequest.RowInput row : request.rows()) {
            if (!activeStudentIds.contains(row.studentId())) {
                throw new ConflictException("Student does not belong to the selected class: " + row.studentId());
            }
            for (SaveAssessmentSheetRequest.CellInput cellInput : row.scores()) {
                AssessmentSheetColumn column = byClientKey.get(cellInput.clientKey());
                if (column == null) {
                    throw new ConflictException("Unknown column clientKey: " + cellInput.clientKey());
                }
                AssessmentSheetScoreType scoreType = typesByCode.get(cellInput.typeCode());
                if (scoreType == null) {
                    throw new ConflictException("Unknown assessment type: " + cellInput.typeCode());
                }
                if (cellInput.score() != null && cellInput.score().compareTo(scoreType.getMaxScore()) > 0) {
                    throw new ConflictException(
                        "Score for \"" + column.getName() + "\" / " + scoreType.getName()
                            + " cannot exceed " + scoreType.getMaxScore());
                }
                AssessmentSheetCell cell = cellRepository
                    .findByColumnIdAndStudentIdAndTypeCode(
                        column.getId(), row.studentId(), scoreType.getCode())
                    .orElseGet(AssessmentSheetCell::new);
                cell.setSheetId(sheet.getId());
                cell.setColumnId(column.getId());
                cell.setStudentId(row.studentId());
                cell.setTypeCode(scoreType.getCode());
                cell.setScore(cellInput.score());
                cell.setRemarks(blankToNull(cellInput.remarks()));
                cellRepository.save(cell);
            }
        }

        auditService.logAdminAction(
            actor,
            "UPSERT",
            "assessment_sheet",
            sheet.getId(),
            type + ",classId=" + request.classId() + ",week=" + week);

        ensureClassSubjectsOnUnpublishedSheet(sheet);
        return buildResponse(type, request.classId(), request.termId(), week, sheet);
    }

    @Transactional
    public AssessmentSheetResponse publishSheet(PublishAssessmentSheetRequest request, SecurityUser actor) {
        AssessmentSheetType type = request.type();
        int week = normalizeWeek(type, request.weekNumber());
        validateClassTerm(request.classId(), request.termId());
        activeTermService.requireMatchesActiveTermForWrite(request.termId(), actor);
        authorizationService.requireTeacherForClass(actor, request.classId());

        AssessmentSheet sheet = assessmentSheetRepository
            .findBySheetTypeAndClassIdAndTermIdAndWeekNumber(
                type, request.classId(), request.termId(), week)
            .orElseThrow(() -> new ConflictException("Save the sheet before publishing"));

        if (type == AssessmentSheetType.MIDTERM) {
            return approveMidterm(sheet, type, request.termId(), week, actor);
        }

        if (request.studentId() != null) {
            publishOneStudent(sheet, type, request.termId(), week, request.studentId(), actor);
        } else {
            publishClassSheet(sheet, type, request.termId(), week, actor);
        }

        return buildResponse(type, request.classId(), request.termId(), week, sheet);
    }

    @Transactional
    public AssessmentSheetResponse submitSheet(PublishAssessmentSheetRequest request, SecurityUser actor) {
        if (request.type() != AssessmentSheetType.MIDTERM) {
            throw new ConflictException("Only midterm reports are submitted to admin for approval");
        }
        if (actor.getRole() != User.Role.TEACHER || actor.getProfileId() == null) {
            throw new ForbiddenException("Only teachers can submit midterm reports to admin");
        }
        int week = normalizeWeek(request.type(), request.weekNumber());
        validateClassTerm(request.classId(), request.termId());
        activeTermService.requireMatchesActiveTermForWrite(request.termId(), actor);
        authorizationService.requireTeacherForClass(actor, request.classId());

        AssessmentSheet sheet = assessmentSheetRepository
            .findBySheetTypeAndClassIdAndTermIdAndWeekNumber(
                request.type(), request.classId(), request.termId(), week)
            .orElseThrow(() -> new ConflictException("Save the midterm sheet before submitting"));
        stripProjectFromMidterm(sheet);
        if (sheet.getPublishedAt() != null) {
            throw new ConflictException("This midterm report is already approved for parents");
        }
        if (sheet.getSubmittedAt() != null) {
            throw new ConflictException("Midterm report already submitted to admin");
        }
        if (cellRepository.findBySheetId(sheet.getId()).isEmpty()) {
            throw new ConflictException("Enter scores for the class before submitting");
        }

        sheet.setSubmittedAt(LocalDateTime.now());
        sheet.setSubmittedByTeacherId(actor.getProfileId());
        sheet = assessmentSheetRepository.save(sheet);
        auditService.logAdminAction(
            actor,
            "SUBMIT",
            "assessment_sheet",
            sheet.getId(),
            "MIDTERM,classId=" + request.classId());
        return buildResponse(request.type(), request.classId(), request.termId(), week, sheet);
    }

    @Transactional
    public AssessmentSheetResponse rejectSheet(PublishAssessmentSheetRequest request, SecurityUser actor) {
        if (actor.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Only admins can send midterm reports back to the teacher");
        }
        if (request.type() != AssessmentSheetType.MIDTERM) {
            throw new ConflictException("Only midterm reports can be sent back for revision");
        }
        int week = normalizeWeek(request.type(), request.weekNumber());
        validateClassTerm(request.classId(), request.termId());
        AssessmentSheet sheet = assessmentSheetRepository
            .findBySheetTypeAndClassIdAndTermIdAndWeekNumber(
                request.type(), request.classId(), request.termId(), week)
            .orElseThrow(() -> new ResourceNotFoundException("Midterm report not found"));
        if (sheet.getPublishedAt() != null) {
            throw new ConflictException("Approved midterm reports cannot be sent back");
        }
        if (sheet.getSubmittedAt() == null) {
            throw new ConflictException("Only submitted midterm reports can be sent back");
        }
        sheet.setSubmittedAt(null);
        sheet.setSubmittedByTeacherId(null);
        sheet = assessmentSheetRepository.save(sheet);
        auditService.logAdminAction(
            actor,
            "REJECT",
            "assessment_sheet",
            sheet.getId(),
            "MIDTERM,classId=" + request.classId());
        return buildResponse(request.type(), request.classId(), request.termId(), week, sheet);
    }

    @Transactional(readOnly = true)
    public List<MidtermSubmissionSummaryResponse> listSubmittedMidterms(SecurityUser actor) {
        if (actor.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Only admins can list submitted midterm reports");
        }
        return assessmentSheetRepository
            .findBySheetTypeAndSubmittedAtIsNotNullAndPublishedAtIsNullOrderBySubmittedAtDesc(
                AssessmentSheetType.MIDTERM)
            .stream()
            .map(this::toMidtermSummary)
            .toList();
    }

    private AssessmentSheetResponse approveMidterm(
        AssessmentSheet sheet,
        AssessmentSheetType type,
        Long termId,
        int week,
        SecurityUser actor) {
        if (actor.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Only admins can approve midterm reports for parents");
        }
        stripProjectFromMidterm(sheet);
        if (sheet.getPublishedAt() != null) {
            return buildResponse(type, sheet.getClassId(), termId, week, sheet);
        }
        if (sheet.getSubmittedAt() == null) {
            throw new ConflictException("The teacher must submit this midterm report before it can be approved");
        }
        publishClassSheet(sheet, type, termId, week, actor);
        AssessmentSheet published = assessmentSheetRepository.findById(sheet.getId()).orElse(sheet);
        return buildResponse(type, sheet.getClassId(), termId, week, published);
    }

    private void publishClassSheet(
        AssessmentSheet sheet,
        AssessmentSheetType type,
        Long termId,
        int week,
        SecurityUser actor) {
        boolean firstPublish = sheet.getPublishedAt() == null;
        if (firstPublish) {
            sheet.setPublishedAt(LocalDateTime.now());
            assessmentSheetRepository.save(sheet);
        }
        auditService.logAdminAction(
            actor,
            "PUBLISH",
            "assessment_sheet",
            sheet.getId(),
            type + ",classId=" + sheet.getClassId() + ",week=" + week);
        if (firstPublish) {
            Set<Long> alreadySent = publicationRepository.findBySheetId(sheet.getId()).stream()
                .map(AssessmentSheetPublication::getStudentId)
                .collect(Collectors.toCollection(HashSet::new));
            Set<Long> studentIds = studentsOnSheet(sheet);
            studentIds.removeAll(alreadySent);
            notifyParentsOfPublishedSheet(sheet, type, termId, week, studentIds, actor);
        }
    }

    private MidtermSubmissionSummaryResponse toMidtermSummary(AssessmentSheet sheet) {
        String className = schoolClassRepository.findById(sheet.getClassId())
            .map(SchoolClass::getName)
            .orElse(null);
        String termName = termRepository.findById(sheet.getTermId())
            .map(Term::getName)
            .orElse(null);
        String teacherName = sheet.getSubmittedByTeacherId() != null
            ? teacherRepository.findById(sheet.getSubmittedByTeacherId())
                .map(Teacher::getFullName)
                .orElse(null)
            : null;
        return new MidtermSubmissionSummaryResponse(
            sheet.getId(),
            sheet.getClassId(),
            className,
            sheet.getTermId(),
            termName,
            sheet.getSessionId(),
            submissionStatus(sheet),
            sheet.getSubmittedAt(),
            sheet.getSubmittedByTeacherId(),
            teacherName,
            activeStudentsInClass(sheet.getClassId()).size());
    }

    private void requireMidtermEditable(AssessmentSheetType type, AssessmentSheet sheet) {
        if (type != AssessmentSheetType.MIDTERM || sheet.getId() == null) {
            return;
        }
        if (sheet.getPublishedAt() != null) {
            throw new ConflictException("Approved midterm reports cannot be edited");
        }
        if (sheet.getSubmittedAt() != null) {
            throw new ConflictException("Submitted midterm reports cannot be edited until admin sends them back");
        }
    }

    private void stripProjectFromMidterm(AssessmentSheet sheet) {
        if (sheet == null || sheet.getId() == null || sheet.getSheetType() != AssessmentSheetType.MIDTERM) {
            return;
        }
        scoreTypeRepository.findBySheetIdAndCode(sheet.getId(), PROJECT_TYPE).ifPresent(type -> {
            cellRepository.deleteBySheetIdAndTypeCode(sheet.getId(), PROJECT_TYPE);
            scoreTypeRepository.delete(type);
        });
    }

    private static boolean isProjectType(String code) {
        return code != null && PROJECT_TYPE.equalsIgnoreCase(code.trim());
    }

    private static String submissionStatus(AssessmentSheet sheet) {
        if (sheet == null) {
            return "DRAFT";
        }
        if (sheet.getPublishedAt() != null) {
            return "PUBLISHED";
        }
        if (sheet.getSubmittedAt() != null) {
            return "SUBMITTED";
        }
        return "DRAFT";
    }

    private void publishOneStudent(
        AssessmentSheet sheet,
        AssessmentSheetType type,
        Long termId,
        int week,
        Long studentId,
        SecurityUser actor) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (!sheet.getClassId().equals(student.getClassId())) {
            throw new ConflictException("Student does not belong to the selected class");
        }
        if (sheet.getPublishedAt() != null
            || publicationRepository.findBySheetIdAndStudentId(sheet.getId(), studentId).isPresent()) {
            return;
        }
        if (cellRepository.findBySheetIdAndStudentId(sheet.getId(), studentId).isEmpty()) {
            throw new ConflictException("Save this pupil's scores before publishing");
        }

        LocalDateTime now = LocalDateTime.now();
        AssessmentSheetPublication publication = new AssessmentSheetPublication();
        publication.setSheetId(sheet.getId());
        publication.setStudentId(studentId);
        publication.setPublishedAt(now);
        publicationRepository.save(publication);

        auditService.logAdminAction(
            actor,
            "PUBLISH",
            "assessment_sheet",
            sheet.getId(),
            type + ",classId=" + sheet.getClassId() + ",week=" + week + ",studentId=" + studentId);

        notifyParentsOfPublishedSheet(sheet, type, termId, week, Set.of(studentId), actor);
    }

    private Set<Long> studentsOnSheet(AssessmentSheet sheet) {
        Set<Long> studentIds = cellRepository.findBySheetId(sheet.getId()).stream()
            .map(AssessmentSheetCell::getStudentId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (studentIds.isEmpty()) {
            for (Student student : activeStudentsInClass(sheet.getClassId())) {
                studentIds.add(student.getId());
            }
        }
        return studentIds;
    }

    private void notifyParentsOfPublishedSheet(
        AssessmentSheet sheet,
        AssessmentSheetType type,
        Long termId,
        int week,
        Set<Long> studentIds,
        SecurityUser actor) {
        if (studentIds == null || studentIds.isEmpty()) {
            return;
        }

        String termName = termRepository.findById(termId).map(Term::getName).orElse("this term");
        String reportLabel = type == AssessmentSheetType.MIDTERM
            ? "Midterm — " + termName
            : "Weekly test week " + week + " — " + termName;
        String portalUrl = frontendProperties.getOrigin().replaceAll("/$", "") + "/parent/results";
        Integer weekNumber = type == AssessmentSheetType.MIDTERM ? null : week;

        Map<Long, EmailAttachment> pdfByStudentId = new HashMap<>();
        for (Long studentId : studentIds) {
            try {
                byte[] pdf = parentPublishedReportService.assessmentPdf(
                    studentId, type, termId, weekNumber, actor);
                String filename = type == AssessmentSheetType.MIDTERM
                    ? "midterm-" + studentId + "-term-" + termId + ".pdf"
                    : "weekly-test-" + studentId + "-week-" + week + ".pdf";
                pdfByStudentId.put(studentId, new EmailAttachment(filename, "application/pdf", pdf));
            } catch (Exception ex) {
                log.warn(
                    "Could not generate assessment PDF for studentId={} type={} termId={} week={}",
                    studentId,
                    type,
                    termId,
                    week,
                    ex);
            }
        }

        parentNotifyService.notifyParentsForStudents(
            studentIds,
            (user, parent) -> {
                List<Long> childIds = parentNotifyService.linkedStudentIdsForParent(parent.getId(), studentIds);
                List<String> childNames = parentNotifyService.childNamesForParent(parent.getId(), studentIds);
                List<EmailAttachment> attachments = childIds.stream()
                    .map(pdfByStudentId::get)
                    .filter(Objects::nonNull)
                    .toList();
                emailService.sendAssessmentSheetPublished(
                    user.getEmail(),
                    parent.getFullName(),
                    String.join(", ", childNames),
                    reportLabel,
                    portalUrl,
                    attachments);
            });
    }

    private Map<String, AssessmentSheetScoreType> upsertScoreTypes(
        AssessmentSheet sheet,
        List<SaveAssessmentSheetRequest.TypeInput> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            throw new ConflictException("At least one assessment type is required");
        }

        Set<String> codes = new HashSet<>();
        int order = 0;
        List<SaveAssessmentSheetRequest.TypeInput> normalized = new ArrayList<>();
        for (SaveAssessmentSheetRequest.TypeInput input : incoming) {
            String code = input.code() != null ? input.code().trim() : "";
            String name = input.name() != null ? input.name().trim() : "";
            if (code.isBlank()) {
                throw new ConflictException("Assessment type code is required");
            }
            if (name.isBlank()) {
                throw new ConflictException("Assessment type name is required");
            }
            if (!codes.add(code)) {
                throw new ConflictException("Duplicate assessment type: " + code);
            }
            if (input.maxScore() == null || input.maxScore().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ConflictException("Assessment type max score must be greater than zero");
            }
            normalized.add(new SaveAssessmentSheetRequest.TypeInput(
                code,
                name,
                input.maxScore(),
                input.sortOrder() != null ? input.sortOrder() : order));
            order++;
        }

        Map<String, AssessmentSheetScoreType> existing = scoreTypeRepository
            .findBySheetIdOrderBySortOrderAscIdAsc(sheet.getId()).stream()
            .collect(Collectors.toMap(AssessmentSheetScoreType::getCode, t -> t, (a, b) -> a, LinkedHashMap::new));

        for (AssessmentSheetScoreType leftover : existing.values()) {
            if (!codes.contains(leftover.getCode())) {
                cellRepository.deleteBySheetIdAndTypeCode(sheet.getId(), leftover.getCode());
                scoreTypeRepository.delete(leftover);
            }
        }

        Map<String, AssessmentSheetScoreType> byCode = new LinkedHashMap<>();
        for (SaveAssessmentSheetRequest.TypeInput input : normalized) {
            AssessmentSheetScoreType type = existing.get(input.code());
            if (type == null) {
                type = new AssessmentSheetScoreType();
                type.setSheetId(sheet.getId());
                type.setCode(input.code());
            }
            type.setName(input.name());
            type.setSortOrder(input.sortOrder());
            type.setMaxScore(input.maxScore());
            type = scoreTypeRepository.save(type);
            byCode.put(type.getCode(), type);
        }
        return byCode;
    }

    private AssessmentSheetResponse buildResponse(
        AssessmentSheetType type,
        Long classId,
        Long termId,
        int week,
        AssessmentSheet sheet) {
        List<AssessmentSheetResponse.ScoreType> types = new ArrayList<>();
        List<AssessmentSheetResponse.Column> columns = new ArrayList<>();
        Map<Long, Map<Long, Map<String, AssessmentSheetCell>>> cellsByStudent = new HashMap<>();
        Map<Long, LocalDateTime> publishedByStudent = new HashMap<>();

        if (sheet != null && sheet.getId() != null) {
            List<AssessmentSheetScoreType> persistedTypes =
                scoreTypeRepository.findBySheetIdOrderBySortOrderAscIdAsc(sheet.getId());
            for (AssessmentSheetScoreType scoreType : persistedTypes) {
                if (type == AssessmentSheetType.MIDTERM && isProjectType(scoreType.getCode())) {
                    continue;
                }
                types.add(new AssessmentSheetResponse.ScoreType(
                    scoreType.getCode(),
                    scoreType.getName(),
                    scoreType.getMaxScore(),
                    scoreType.getSortOrder()));
            }
            List<AssessmentSheetColumn> cols =
                columnRepository.findBySheetIdOrderBySortOrderAscIdAsc(sheet.getId());
            for (AssessmentSheetColumn col : cols) {
                columns.add(new AssessmentSheetResponse.Column(
                    col.getId(),
                    col.getName(),
                    col.getSortOrder(),
                    col.getMaxScore()));
            }
            for (AssessmentSheetCell cell : cellRepository.findBySheetId(sheet.getId())) {
                cellsByStudent
                    .computeIfAbsent(cell.getStudentId(), id -> new HashMap<>())
                    .computeIfAbsent(cell.getColumnId(), id -> new HashMap<>())
                    .put(cell.getTypeCode(), cell);
            }
            if (sheet.getPublishedAt() == null) {
                for (AssessmentSheetPublication publication : publicationRepository.findBySheetId(sheet.getId())) {
                    publishedByStudent.put(publication.getStudentId(), publication.getPublishedAt());
                }
            }
        }

        if (types.isEmpty()) {
            for (DefaultScoreType def : defaultTypes(type)) {
                types.add(new AssessmentSheetResponse.ScoreType(
                    def.code(),
                    def.name(),
                    DEFAULT_MAX,
                    def.sortOrder()));
            }
        }

        if (columns.isEmpty()) {
            columns.addAll(seedSubjectColumns(classId));
        }

        List<AssessmentSheetResponse.PupilRow> pupils = new ArrayList<>();
        for (Student student : activeStudentsInClass(classId)) {
            Map<String, Map<String, AssessmentSheetResponse.CellValue>> scores = new LinkedHashMap<>();
            Map<Long, Map<String, AssessmentSheetCell>> studentCells =
                cellsByStudent.getOrDefault(student.getId(), Map.of());
            for (AssessmentSheetResponse.Column col : columns) {
                String scoreKey = col.id() != null ? String.valueOf(col.id()) : "seed-" + col.sortOrder();
                Map<String, AssessmentSheetCell> byType =
                    col.id() != null ? studentCells.getOrDefault(col.id(), Map.of()) : Map.of();
                Map<String, AssessmentSheetResponse.CellValue> typeScores = new LinkedHashMap<>();
                for (AssessmentSheetResponse.ScoreType scoreType : types) {
                    AssessmentSheetCell cell = byType.get(scoreType.code());
                    typeScores.put(
                        scoreType.code(),
                        new AssessmentSheetResponse.CellValue(
                            cell != null ? cell.getScore() : null,
                            cell != null ? cell.getRemarks() : null));
                }
                scores.put(scoreKey, typeScores);
            }
            LocalDateTime pupilPublishedAt = sheet != null ? sheet.getPublishedAt() : null;
            if (pupilPublishedAt == null) {
                pupilPublishedAt = publishedByStudent.get(student.getId());
            }
            pupils.add(new AssessmentSheetResponse.PupilRow(
                student.getId(),
                formatStudentName(student),
                student.getAdmissionNumber(),
                scores,
                pupilPublishedAt));
        }

        return new AssessmentSheetResponse(
            sheet != null ? sheet.getId() : null,
            type,
            classId,
            termId,
            week,
            sheet != null ? sheet.getPublishedAt() : null,
            sheet != null ? sheet.getSubmittedAt() : null,
            sheet != null ? sheet.getSubmittedByTeacherId() : null,
            submissionStatus(sheet),
            types,
            columns,
            pupils);
    }

    @Transactional
    public void addSubjectToUnpublishedCurrentSheets(Long classId, String subjectName) {
        if (classId == null || subjectName == null || subjectName.isBlank()) {
            return;
        }
        Long termId = currentActiveTermId();
        if (termId == null) {
            return;
        }
        for (AssessmentSheet sheet : unpublishedCurrentSheets(classId, termId)) {
            addColumnIfMissing(sheet, subjectName.trim());
        }
    }

    @Transactional
    public void removeSubjectFromUnpublishedCurrentSheets(Long classId, String subjectName) {
        if (classId == null || subjectName == null || subjectName.isBlank()) {
            return;
        }
        Long termId = currentActiveTermId();
        if (termId == null) {
            return;
        }
        String wanted = subjectName.trim();
        for (AssessmentSheet sheet : unpublishedCurrentSheets(classId, termId)) {
            for (AssessmentSheetColumn column : columnRepository.findBySheetIdOrderBySortOrderAscIdAsc(sheet.getId())) {
                if (column.getName() != null && column.getName().trim().equalsIgnoreCase(wanted)) {
                    cellRepository.deleteByColumnId(column.getId());
                    columnRepository.delete(column);
                }
            }
        }
    }

    private void ensureClassSubjectsOnUnpublishedSheet(AssessmentSheet sheet) {
        if (sheet == null || sheet.getId() == null || sheet.getPublishedAt() != null) {
            return;
        }
        if (sheet.getSheetType() == AssessmentSheetType.MIDTERM && sheet.getSubmittedAt() != null) {
            return;
        }
        Long activeTermId = currentActiveTermId();
        if (activeTermId == null || !activeTermId.equals(sheet.getTermId())) {
            return;
        }
        for (AssessmentSheetResponse.Column seeded : seedSubjectColumns(sheet.getClassId())) {
            addColumnIfMissing(sheet, seeded.name());
        }
    }

    private void addColumnIfMissing(AssessmentSheet sheet, String subjectName) {
        if (sheet.getPublishedAt() != null || subjectName == null || subjectName.isBlank()) {
            return;
        }
        List<AssessmentSheetColumn> existing = columnRepository.findBySheetIdOrderBySortOrderAscIdAsc(sheet.getId());
        for (AssessmentSheetColumn column : existing) {
            if (column.getName() != null && column.getName().trim().equalsIgnoreCase(subjectName.trim())) {
                return;
            }
        }
        int nextOrder = existing.stream()
            .map(AssessmentSheetColumn::getSortOrder)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .max()
            .orElse(-1) + 1;
        BigDecimal maxScore = existing.stream()
            .map(AssessmentSheetColumn::getMaxScore)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(DEFAULT_MAX);
        AssessmentSheetColumn column = new AssessmentSheetColumn();
        column.setSheetId(sheet.getId());
        column.setName(subjectName.trim());
        column.setSortOrder(nextOrder);
        column.setMaxScore(maxScore);
        columnRepository.save(column);
    }

    private List<AssessmentSheet> unpublishedCurrentSheets(Long classId, Long termId) {
        return assessmentSheetRepository.findByClassIdAndTermId(classId, termId).stream()
            .filter(sheet -> sheet.getPublishedAt() == null)
            .filter(sheet -> sheet.getSheetType() != AssessmentSheetType.MIDTERM || sheet.getSubmittedAt() == null)
            .toList();
    }

    private Long currentActiveTermId() {
        try {
            return activeTermService.requireActiveTermId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private List<AssessmentSheetResponse.Column> seedSubjectColumns(Long classId) {
        Map<Long, String> subjectNames = subjectRepository.findAll().stream()
            .collect(Collectors.toMap(Subject::getId, Subject::getName, (a, b) -> a));
        List<String> names = classSubjectRepository.findByClassIdAndIsActiveTrue(classId).stream()
            .map(ClassSubject::getSubjectId)
            .map(id -> subjectNames.getOrDefault(id, null))
            .filter(name -> name != null && !name.isBlank())
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
        List<AssessmentSheetResponse.Column> seeded = new ArrayList<>();
        int order = 0;
        for (String name : names) {
            seeded.add(new AssessmentSheetResponse.Column(
                null,
                name,
                order,
                DEFAULT_MAX));
            order++;
        }
        return seeded;
    }

    private static List<DefaultScoreType> defaultTypes(AssessmentSheetType type) {
        if (type == AssessmentSheetType.MIDTERM) {
            return List.of(
                new DefaultScoreType("CLASS_WORK", "Class work", 0),
                new DefaultScoreType("HOMEWORK", "Homework", 1),
                new DefaultScoreType("TEST", "Test", 2));
        }
        return List.of(new DefaultScoreType("SCORE", "Score", 0));
    }

    private int normalizeWeek(AssessmentSheetType type, Integer weekNumber) {
        if (type == AssessmentSheetType.MIDTERM) {
            return 0;
        }
        if (weekNumber == null || weekNumber < 1 || weekNumber > 16) {
            throw new ConflictException("Week number must be between 1 and 16 for weekly tests");
        }
        return weekNumber;
    }

    private void validateClassTerm(Long classId, Long termId) {
        authorizationService.requireClassGroup(classId, ClassGroup.PRIMARY);
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

    private record DefaultScoreType(String code, String name, int sortOrder) {
    }
}
