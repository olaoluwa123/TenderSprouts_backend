package com.example.backend.service;

import com.example.backend.dto.grade.CreateGradeRequest;
import com.example.backend.dto.grade.GradeResponse;
import com.example.backend.dto.grade.GradingSheetAssessmentTypeResponse;
import com.example.backend.dto.grade.GradingSheetCellResponse;
import com.example.backend.dto.grade.GradingSheetResponse;
import com.example.backend.dto.grade.GradingSheetSubjectRowResponse;
import com.example.backend.dto.grade.SaveStudentGradesRequest;
import com.example.backend.entity.AssessmentTypeLookup;
import com.example.backend.entity.ClassSubject;
import com.example.backend.entity.Grade;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Student;
import com.example.backend.entity.Subject;
import com.example.backend.entity.Term;
import com.example.backend.entity.TermResult;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.ClassGroup;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ClassSubjectRepository;
import com.example.backend.repository.GradeRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.repository.TermResultRepository;
import com.example.backend.security.SecurityUser;
import com.example.backend.util.GradeScoringUtil;
import com.example.backend.util.LetterGradeUtil;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradeService {

    private final GradeRepository gradeRepository;
    private final StudentRepository studentRepository;
    private final TermRepository termRepository;
    private final TeacherRepository teacherRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ActiveSessionService activeSessionService;
    private final ActiveTermService activeTermService;
    private final AuthorizationService authorizationService;
    private final AssessmentTypeService assessmentTypeService;
    private final TermResultRepository termResultRepository;
    private final AuditService auditService;

    public GradeService(
        GradeRepository gradeRepository,
        StudentRepository studentRepository,
        TermRepository termRepository,
        TeacherRepository teacherRepository,
        ClassSubjectRepository classSubjectRepository,
        SubjectRepository subjectRepository,
        SchoolClassRepository schoolClassRepository,
        ActiveSessionService activeSessionService,
        ActiveTermService activeTermService,
        AuthorizationService authorizationService,
        AssessmentTypeService assessmentTypeService,
        TermResultRepository termResultRepository,
        AuditService auditService) {
        this.gradeRepository = gradeRepository;
        this.studentRepository = studentRepository;
        this.termRepository = termRepository;
        this.teacherRepository = teacherRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.subjectRepository = subjectRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.activeSessionService = activeSessionService;
        this.activeTermService = activeTermService;
        this.authorizationService = authorizationService;
        this.assessmentTypeService = assessmentTypeService;
        this.termResultRepository = termResultRepository;
        this.auditService = auditService;
    }

    @Transactional
    public List<GradeResponse> createBatch(List<CreateGradeRequest> requests, SecurityUser actor) {
        List<GradeResponse> results = new ArrayList<>();
        for (CreateGradeRequest request : requests) {
            results.add(createSingle(request, actor));
        }
        return results;
    }

    @Transactional
    public GradeResponse createSingle(CreateGradeRequest request, SecurityUser actor) {
        if (!studentRepository.existsById(request.studentId())) {
            throw new ResourceNotFoundException("Student not found");
        }
        if (!termRepository.existsById(request.termId())) {
            throw new ResourceNotFoundException("Term not found");
        }
        requireStudentInAssignedClass(actor, request.studentId(), request.classId());
        requireTermInActiveSession(request.termId());
        activeTermService.requireMatchesActiveTermForWrite(request.termId(), actor);
        AssessmentTypeLookup assessmentType = assessmentTypeService.requireActiveType(request.assessmentType());
        String typeCode = assessmentType.getCode();
        if (gradeRepository.findByStudentIdAndSubjectIdAndTermIdAndAssessmentType(
            request.studentId(), request.subjectId(), request.termId(), typeCode).isPresent()) {
            throw new ConflictException("Grade already exists for this assessment");
        }
        BigDecimal maxScore = assessmentType.getDefaultMaxScore();
        if (request.score() != null && request.score().compareTo(maxScore) > 0) {
            throw new ConflictException("Score cannot exceed max score of " + maxScore);
        }
        Long teacherId = resolveTeacherId(actor);
        Grade grade = new Grade();
        grade.setStudentId(request.studentId());
        grade.setSubjectId(request.subjectId());
        grade.setClassId(request.classId());
        grade.setTermId(request.termId());
        grade.setSessionId(activeSessionService.requireActiveSessionId());
        grade.setAssessmentType(typeCode);
        grade.setScore(request.score());
        grade.setMaxScore(maxScore);
        grade.setRemarks(request.remarks());
        grade.setTeacherId(teacherId);
        return GradeResponse.from(gradeRepository.save(grade));
    }

    public GradingSheetResponse getGradingSheet(Long studentId, Long termId, SecurityUser actor) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (!termRepository.existsById(termId)) {
            throw new ResourceNotFoundException("Term not found");
        }
        Long classId = student.getClassId();
        if (classId == null) {
            throw new ConflictException("Student is not assigned to a class");
        }
        if (actor.getRole() == User.Role.TEACHER) {
            authorizationService.requireTeacherForClass(actor, classId);
            requirePrimaryClassForTeacher(actor, classId);
        } else if (actor.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Not allowed to view grading sheet");
        }

        String className = schoolClassRepository.findById(classId)
            .map(SchoolClass::getName)
            .orElse(null);

        List<AssessmentTypeLookup> weightedTypes = assessmentTypeService.listWeightedActiveEntities();
        Set<String> weightedTypeCodes = GradeScoringUtil.weightedTypeCodes(weightedTypes);
        List<GradingSheetAssessmentTypeResponse> assessmentColumns = weightedTypes.stream()
            .map(GradingSheetAssessmentTypeResponse::from)
            .toList();

        List<String> warnings = new ArrayList<>();
        BigDecimal maxSum = GradeScoringUtil.sumMaxScores(weightedTypes);
        if (maxSum.compareTo(new BigDecimal("100")) != 0) {
            warnings.add("Active assessment max scores sum to "
                + maxSum.toPlainString() + " but should total 100");
        }

        List<ClassSubject> classSubjects = classSubjectRepository.findByClassId(classId).stream()
            .filter(cs -> Boolean.TRUE.equals(cs.getIsActive()))
            .toList();
        if (classSubjects.isEmpty()) {
            warnings.add(
                "No subjects are assigned to this class. An admin must add subjects under Classes & Subjects before grades can be entered.");
        }
        Map<Long, String> subjectNames = subjectRepository.findAllById(
            classSubjects.stream().map(ClassSubject::getSubjectId).toList()).stream()
            .collect(Collectors.toMap(Subject::getId, Subject::getName));

        List<Grade> existingGrades = gradeRepository.findByStudentIdAndTermId(studentId, termId);
        Map<Long, List<Grade>> gradesBySubject = existingGrades.stream()
            .collect(Collectors.groupingBy(Grade::getSubjectId));

        List<GradingSheetSubjectRowResponse> subjectRows = new ArrayList<>();
        for (ClassSubject classSubject : classSubjects) {
            Long subjectId = classSubject.getSubjectId();
            List<Grade> subjectGrades = gradesBySubject.getOrDefault(subjectId, List.of());
            Map<String, Grade> gradeByType = subjectGrades.stream()
                .collect(Collectors.toMap(Grade::getAssessmentType, g -> g, (a, b) -> a));

            List<GradingSheetCellResponse> cells = new ArrayList<>();
            for (AssessmentTypeLookup type : weightedTypes) {
                Grade grade = gradeByType.get(type.getCode());
                cells.add(new GradingSheetCellResponse(
                    type.getCode(),
                    grade != null ? grade.getScore() : null,
                    type.getDefaultMaxScore()));
            }

            BigDecimal total = GradeScoringUtil.computeSubjectTotal(subjectGrades, weightedTypeCodes);
            subjectRows.add(new GradingSheetSubjectRowResponse(
                subjectId,
                subjectNames.get(subjectId),
                cells,
                total,
                LetterGradeUtil.toLetter(total)));
        }

        String studentName = formatStudentName(student);
        return new GradingSheetResponse(
            studentId,
            studentName,
            classId,
            className,
            termId,
            assessmentColumns,
            subjectRows,
            warnings);
    }

    @Transactional
    public GradingSheetResponse upsertStudentGrades(
        Long studentId,
        SaveStudentGradesRequest request,
        SecurityUser actor) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found");
        }
        if (!termRepository.existsById(request.termId())) {
            throw new ResourceNotFoundException("Term not found");
        }
        requireStudentInAssignedClass(actor, studentId, request.classId());
        requireTermInActiveSession(request.termId());
        activeTermService.requireMatchesActiveTermForWrite(request.termId(), actor);

        Long teacherId = resolveTeacherId(actor);
        Long sessionId = activeSessionService.requireActiveSessionId();

        for (SaveStudentGradesRequest.GradeEntryRequest entry : request.entries()) {
            if (entry.score() == null) {
                continue;
            }
            AssessmentTypeLookup assessmentType = assessmentTypeService.requireActiveType(entry.assessmentType());
            String typeCode = assessmentType.getCode();
            BigDecimal maxScore = assessmentType.getDefaultMaxScore();
            if (entry.score().compareTo(maxScore) > 0) {
                throw new ConflictException("Score cannot exceed max score of " + maxScore);
            }

            Grade grade = gradeRepository.findByStudentIdAndSubjectIdAndTermIdAndAssessmentType(
                studentId, entry.subjectId(), request.termId(), typeCode)
                .orElseGet(Grade::new);
            grade.setStudentId(studentId);
            grade.setSubjectId(entry.subjectId());
            grade.setClassId(request.classId());
            grade.setTermId(request.termId());
            grade.setSessionId(sessionId);
            grade.setAssessmentType(typeCode);
            grade.setScore(entry.score());
            grade.setMaxScore(maxScore);
            grade.setTeacherId(teacherId);
            gradeRepository.save(grade);
        }

        Student student = studentRepository.findById(studentId).orElse(null);
        String pupilName = student != null ? formatStudentName(student) : null;
        auditService.logAdminAction(actor, "UPDATE", "grades", studentId, pupilName);

        return getGradingSheet(studentId, request.termId(), actor);
    }

    public List<GradeResponse> listForStudent(Long studentId, Long termId, SecurityUser actor) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found");
        }
        if (actor.getRole() == User.Role.PARENT) {
            authorizationService.requireParentAccessToStudent(actor, studentId);
            return listPublishedGradesForParent(studentId, termId);
        }
        if (actor.getRole() == User.Role.TEACHER) {
            authorizationService.requireTeacherAccessToStudent(actor, studentId);
        } else if (actor.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Not allowed to view grades");
        }
        List<Grade> grades = termId != null
            ? gradeRepository.findByStudentIdAndTermId(studentId, termId)
            : gradeRepository.findByStudentId(studentId);
        return grades.stream().map(GradeResponse::from).toList();
    }

    private List<GradeResponse> listPublishedGradesForParent(Long studentId, Long termId) {
        Set<Long> approvedTermIds = termResultRepository.findByStudentIdAndPublishedAtIsNotNull(studentId).stream()
            .map(result -> result.getTermId())
            .collect(Collectors.toCollection(HashSet::new));
        if (approvedTermIds.isEmpty()) {
            return List.of();
        }
        if (termId != null) {
            if (!approvedTermIds.contains(termId)) {
                throw new ForbiddenException("Grades are not available until admin approves term results");
            }
            return gradeRepository.findByStudentIdAndTermId(studentId, termId).stream()
                .map(GradeResponse::from)
                .toList();
        }
        return gradeRepository.findByStudentId(studentId).stream()
            .filter(grade -> approvedTermIds.contains(grade.getTermId()))
            .map(GradeResponse::from)
            .toList();
    }

    @Transactional
    public void removeUnpublishedCurrentTermGrades(Long classId, Long subjectId) {
        Long termId;
        try {
            termId = activeTermService.requireActiveTermId();
        } catch (RuntimeException ex) {
            return;
        }
        Set<Long> publishedStudentIds = termResultRepository
            .findByClassIdAndTermIdAndPublishedAtIsNotNull(classId, termId).stream()
            .map(TermResult::getStudentId)
            .collect(Collectors.toSet());
        List<Grade> toDelete = gradeRepository.findByClassIdAndTermIdAndSubjectId(classId, termId, subjectId).stream()
            .filter(grade -> !publishedStudentIds.contains(grade.getStudentId()))
            .toList();
        if (!toDelete.isEmpty()) {
            gradeRepository.deleteAll(toDelete);
        }
    }

    public List<GradeResponse> listForClass(Long classId, Long termId, Long subjectId, SecurityUser actor) {
        authorizationService.requireTeacherForClass(actor, classId);
        requirePrimaryClassForTeacher(actor, classId);
        return gradeRepository.findByClassIdAndTermIdAndSubjectId(classId, termId, subjectId).stream()
            .map(GradeResponse::from)
            .toList();
    }

    private void requireStudentInAssignedClass(SecurityUser actor, Long studentId, Long classId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (!Objects.equals(student.getClassId(), classId)) {
            throw new ForbiddenException("Student is not in this class");
        }
        authorizationService.requireTeacherForClass(actor, classId);
        requirePrimaryClassForTeacher(actor, classId);
    }

    private void requirePrimaryClassForTeacher(SecurityUser actor, Long classId) {
        if (actor.getRole() == User.Role.TEACHER) {
            authorizationService.requireClassGroup(classId, ClassGroup.PRIMARY);
        }
    }

    private void requireTermInActiveSession(Long termId) {
        Term term = termRepository.findById(termId)
            .orElseThrow(() -> new ResourceNotFoundException("Term not found"));
        Long activeSessionId = activeSessionService.requireActiveSessionId();
        if (!term.getSessionId().equals(activeSessionId)) {
            throw new ConflictException("Term does not belong to the active academic session");
        }
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
}
