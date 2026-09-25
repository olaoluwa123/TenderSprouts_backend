package com.example.backend.service;

import com.example.backend.dto.assessment.AssessmentSheetResponse;
import com.example.backend.dto.assessment.PublishAssessmentSheetRequest;
import com.example.backend.dto.assessment.SaveAssessmentSheetRequest;
import com.example.backend.entity.AssessmentSheet;
import com.example.backend.entity.AssessmentSheetCell;
import com.example.backend.entity.AssessmentSheetColumn;
import com.example.backend.entity.AssessmentSheetScoreType;
import com.example.backend.entity.ClassSubject;
import com.example.backend.entity.Student;
import com.example.backend.entity.Subject;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.AssessmentSheetType;
import com.example.backend.entity.enums.ClassGroup;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AssessmentSheetCellRepository;
import com.example.backend.repository.AssessmentSheetColumnRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssessmentSheetService {

    private static final BigDecimal DEFAULT_MAX = new BigDecimal("100");

    private final AssessmentSheetRepository assessmentSheetRepository;
    private final AssessmentSheetColumnRepository columnRepository;
    private final AssessmentSheetCellRepository cellRepository;
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

    public AssessmentSheetService(
        AssessmentSheetRepository assessmentSheetRepository,
        AssessmentSheetColumnRepository columnRepository,
        AssessmentSheetCellRepository cellRepository,
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
        AuditService auditService) {
        this.assessmentSheetRepository = assessmentSheetRepository;
        this.columnRepository = columnRepository;
        this.cellRepository = cellRepository;
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
    }

    @Transactional(readOnly = true)
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
        return buildResponse(type, classId, termId, week, sheet);
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
        sheet.setSheetType(type);
        sheet.setClassId(request.classId());
        sheet.setTermId(request.termId());
        sheet.setWeekNumber(week);
        sheet.setSessionId(sessionId);
        sheet.setTeacherId(teacherId);
        sheet = assessmentSheetRepository.save(sheet);

        Map<String, AssessmentSheetScoreType> typesByCode = upsertScoreTypes(sheet, request.types());

        List<AssessmentSheetColumn> existingColumns =
            columnRepository.findBySheetIdOrderBySortOrderAscIdAsc(sheet.getId());
        Map<Long, AssessmentSheetColumn> existingById = existingColumns.stream()
            .collect(Collectors.toMap(AssessmentSheetColumn::getId, c -> c, (a, b) -> a));

        Set<Long> keepIds = request.columns().stream()
            .map(SaveAssessmentSheetRequest.ColumnInput::id)
            .filter(id -> id != null)
            .collect(Collectors.toSet());

        for (AssessmentSheetColumn existing : existingColumns) {
            if (!keepIds.contains(existing.getId())) {
                cellRepository.deleteByColumnId(existing.getId());
                columnRepository.delete(existing);
            }
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
        if (sheet.getPublishedAt() == null) {
            sheet.setPublishedAt(LocalDateTime.now());
            sheet = assessmentSheetRepository.save(sheet);
        }

        auditService.logAdminAction(
            actor,
            "PUBLISH",
            "assessment_sheet",
            sheet.getId(),
            type + ",classId=" + request.classId() + ",week=" + week);

        return buildResponse(type, request.classId(), request.termId(), week, sheet);
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

        if (sheet != null && sheet.getId() != null) {
            List<AssessmentSheetScoreType> persistedTypes =
                scoreTypeRepository.findBySheetIdOrderBySortOrderAscIdAsc(sheet.getId());
            for (AssessmentSheetScoreType scoreType : persistedTypes) {
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
            pupils.add(new AssessmentSheetResponse.PupilRow(
                student.getId(),
                formatStudentName(student),
                student.getAdmissionNumber(),
                scores));
        }

        return new AssessmentSheetResponse(
            sheet != null ? sheet.getId() : null,
            type,
            classId,
            termId,
            week,
            sheet != null ? sheet.getPublishedAt() : null,
            types,
            columns,
            pupils);
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
                new DefaultScoreType("TEST", "Test", 2),
                new DefaultScoreType("PROJECT", "Project", 3));
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
