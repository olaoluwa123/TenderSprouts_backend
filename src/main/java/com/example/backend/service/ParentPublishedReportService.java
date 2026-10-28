package com.example.backend.service;

import com.example.backend.dto.parentreport.PublishedStudentReportItem;
import com.example.backend.dto.parentreport.StudentAssessmentReportResponse;
import com.example.backend.dto.parentreport.StudentBehaviouralReportView;
import com.example.backend.dto.parentreport.StudentPreschoolReportResponse;
import com.example.backend.entity.AcademicSession;
import com.example.backend.entity.AssessmentSheet;
import com.example.backend.entity.AssessmentSheetCell;
import com.example.backend.entity.AssessmentSheetPublication;
import com.example.backend.entity.AssessmentSheetColumn;
import com.example.backend.entity.AssessmentSheetScoreType;
import com.example.backend.entity.BehaviouralReport;
import com.example.backend.entity.PreschoolReport;
import com.example.backend.entity.PreschoolReportComment;
import com.example.backend.entity.PreschoolReportSummary;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Student;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.TeacherClass;
import com.example.backend.entity.Term;
import com.example.backend.entity.enums.AssessmentSheetType;
import com.example.backend.entity.enums.AttendanceStatus;
import com.example.backend.entity.enums.BehaviourPeriodType;
import com.example.backend.entity.enums.BehaviourRating;
import com.example.backend.entity.enums.Gender;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AcademicSessionRepository;
import com.example.backend.repository.AssessmentSheetCellRepository;
import com.example.backend.repository.AssessmentSheetColumnRepository;
import com.example.backend.repository.AssessmentSheetPublicationRepository;
import com.example.backend.repository.AssessmentSheetRepository;
import com.example.backend.repository.AssessmentSheetScoreTypeRepository;
import com.example.backend.repository.AttendanceRepository;
import com.example.backend.repository.BehaviouralReportRepository;
import com.example.backend.repository.PreschoolReportCommentRepository;
import com.example.backend.repository.PreschoolReportRepository;
import com.example.backend.repository.PreschoolReportSummaryRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.security.SecurityUser;
import com.example.backend.util.StudentAgeUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParentPublishedReportService {

    private static final DateTimeFormatter REPORT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final StudentRepository studentRepository;
    private final TermRepository termRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final AssessmentSheetRepository assessmentSheetRepository;
    private final AssessmentSheetPublicationRepository publicationRepository;
    private final AssessmentSheetColumnRepository columnRepository;
    private final AssessmentSheetScoreTypeRepository scoreTypeRepository;
    private final AssessmentSheetCellRepository cellRepository;
    private final BehaviouralReportRepository behaviouralReportRepository;
    private final PreschoolReportRepository preschoolReportRepository;
    private final PreschoolReportCommentRepository preschoolCommentRepository;
    private final PreschoolReportSummaryRepository preschoolSummaryRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final TeacherRepository teacherRepository;
    private final AttendanceRepository attendanceRepository;
    private final AuthorizationService authorizationService;
    private final PublishedStudentReportPdfGenerator pdfGenerator;

    public ParentPublishedReportService(
        StudentRepository studentRepository,
        TermRepository termRepository,
        AcademicSessionRepository academicSessionRepository,
        SchoolClassRepository schoolClassRepository,
        AssessmentSheetRepository assessmentSheetRepository,
        AssessmentSheetPublicationRepository publicationRepository,
        AssessmentSheetColumnRepository columnRepository,
        AssessmentSheetScoreTypeRepository scoreTypeRepository,
        AssessmentSheetCellRepository cellRepository,
        BehaviouralReportRepository behaviouralReportRepository,
        PreschoolReportRepository preschoolReportRepository,
        PreschoolReportCommentRepository preschoolCommentRepository,
        PreschoolReportSummaryRepository preschoolSummaryRepository,
        TeacherClassRepository teacherClassRepository,
        TeacherRepository teacherRepository,
        AttendanceRepository attendanceRepository,
        AuthorizationService authorizationService,
        PublishedStudentReportPdfGenerator pdfGenerator) {
        this.studentRepository = studentRepository;
        this.termRepository = termRepository;
        this.academicSessionRepository = academicSessionRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.assessmentSheetRepository = assessmentSheetRepository;
        this.publicationRepository = publicationRepository;
        this.columnRepository = columnRepository;
        this.scoreTypeRepository = scoreTypeRepository;
        this.cellRepository = cellRepository;
        this.behaviouralReportRepository = behaviouralReportRepository;
        this.preschoolReportRepository = preschoolReportRepository;
        this.preschoolCommentRepository = preschoolCommentRepository;
        this.preschoolSummaryRepository = preschoolSummaryRepository;
        this.teacherClassRepository = teacherClassRepository;
        this.teacherRepository = teacherRepository;
        this.attendanceRepository = attendanceRepository;
        this.authorizationService = authorizationService;
        this.pdfGenerator = pdfGenerator;
    }

    @Transactional(readOnly = true)
    public List<PublishedStudentReportItem> listPublished(Long studentId, SecurityUser actor) {
        Student student = requireStudent(studentId, actor);
        List<PublishedStudentReportItem> items = new ArrayList<>();

        for (AssessmentSheet sheet : publishedSheetsFor(student)) {
            items.add(toItem(
                sheet.getSheetType().name(),
                sheet.getTermId(),
                sheet.getSessionId(),
                sheet.getClassId(),
                sheet.getWeekNumber(),
                null,
                publishedAtFor(sheet, student.getId()),
                null));
        }
        for (BehaviouralReport report : behaviouralReportRepository
            .findByStudentIdAndPublishedAtIsNotNullOrderByTermIdAscPeriodTypeAscWeekNumberAsc(studentId)) {
            items.add(toItem(
                "BEHAVIOURAL",
                report.getTermId(),
                report.getSessionId(),
                report.getClassId(),
                report.getWeekNumber(),
                report.getPeriodType().name(),
                report.getPublishedAt(),
                null));
        }
        for (PreschoolReport report : publishedPreschoolReportsFor(student)) {
            items.add(toItem(
                "PRESCHOOL",
                report.getTermId(),
                report.getSessionId(),
                report.getClassId(),
                null,
                null,
                report.getPublishedAt(),
                report.getMonthNumber()));
        }

        items.sort(Comparator
            .comparing(PublishedStudentReportItem::publishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(PublishedStudentReportItem::kind));
        return items;
    }

    @Transactional(readOnly = true)
    public StudentAssessmentReportResponse getAssessmentReport(
        Long studentId,
        AssessmentSheetType type,
        Long termId,
        Integer weekNumber,
        SecurityUser actor) {
        Student student = requireStudent(studentId, actor);
        int week = type == AssessmentSheetType.MIDTERM ? 0 : requireWeek(weekNumber);
        AssessmentSheet sheet = requirePublishedSheet(student, type, termId, week);
        return buildAssessment(student, sheet);
    }

    @Transactional(readOnly = true)
    public byte[] assessmentPdf(
        Long studentId,
        AssessmentSheetType type,
        Long termId,
        Integer weekNumber,
        SecurityUser actor) {
        return pdfGenerator.generateAssessment(
            getAssessmentReport(studentId, type, termId, weekNumber, actor));
    }

    @Transactional(readOnly = true)
    public StudentBehaviouralReportView getBehaviouralReport(
        Long studentId,
        Long termId,
        BehaviourPeriodType periodType,
        Integer weekNumber,
        SecurityUser actor) {
        Student student = requireStudent(studentId, actor);
        int week = periodType == BehaviourPeriodType.END_OF_TERM ? 0 : requireWeek(weekNumber);
        BehaviouralReport report = behaviouralReportRepository
            .findByStudentIdAndTermIdAndPeriodTypeAndWeekNumber(studentId, termId, periodType, week)
            .orElseThrow(() -> new ResourceNotFoundException("Behavioural report not found"));
        if (report.getPublishedAt() == null) {
            throw new ForbiddenException("This report is not published yet");
        }
        return buildBehavioural(student, report);
    }

    @Transactional(readOnly = true)
    public byte[] behaviouralPdf(
        Long studentId,
        Long termId,
        BehaviourPeriodType periodType,
        Integer weekNumber,
        SecurityUser actor) {
        return pdfGenerator.generateBehavioural(
            getBehaviouralReport(studentId, termId, periodType, weekNumber, actor));
    }

    @Transactional(readOnly = true)
    public StudentPreschoolReportResponse getPreschoolReport(
        Long studentId,
        Long termId,
        Integer monthNumber,
        SecurityUser actor) {
        Student student = requireStudent(studentId, actor);
        PreschoolReport report = requirePublishedPreschool(student, termId, PreschoolReportService.requireMonth(monthNumber));
        return buildPreschool(student, report);
    }

    @Transactional(readOnly = true)
    public byte[] preschoolPdf(Long studentId, Long termId, Integer monthNumber, SecurityUser actor) {
        return pdfGenerator.generatePreschool(getPreschoolReport(studentId, termId, monthNumber, actor));
    }

    @Transactional(readOnly = true)
    public StudentAssessmentReportResponse previewAssessment(Student student, AssessmentSheet sheet) {
        return buildAssessment(student, sheet);
    }

    public byte[] previewAssessmentPdf(StudentAssessmentReportResponse report) {
        return pdfGenerator.generateAssessment(report);
    }

    private Student requireStudent(Long studentId, SecurityUser actor) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        authorizationService.requireParentOrStaffForStudentRead(actor, studentId);
        return student;
    }

    private Set<AssessmentSheet> publishedSheetsFor(Student student) {
        Map<Long, AssessmentSheet> byId = new LinkedHashMap<>();
        if (student.getClassId() != null) {
            for (AssessmentSheet sheet : assessmentSheetRepository
                .findByClassIdAndPublishedAtIsNotNull(student.getClassId())) {
                byId.put(sheet.getId(), sheet);
            }
        }
        Set<Long> sheetIds = cellRepository.findByStudentId(student.getId()).stream()
            .map(AssessmentSheetCell::getSheetId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        for (AssessmentSheet sheet : assessmentSheetRepository.findAllById(sheetIds)) {
            if (isPublishedForStudent(sheet, student.getId())) {
                byId.put(sheet.getId(), sheet);
            }
        }
        for (AssessmentSheetPublication publication : publicationRepository.findByStudentId(student.getId())) {
            assessmentSheetRepository.findById(publication.getSheetId())
                .ifPresent(sheet -> byId.put(sheet.getId(), sheet));
        }
        return new LinkedHashSet<>(byId.values());
    }

    private LocalDateTime publishedAtFor(AssessmentSheet sheet, Long studentId) {
        if (sheet.getPublishedAt() != null) {
            return sheet.getPublishedAt();
        }
        return publicationRepository.findBySheetIdAndStudentId(sheet.getId(), studentId)
            .map(AssessmentSheetPublication::getPublishedAt)
            .orElse(null);
    }

    private boolean isPublishedForStudent(AssessmentSheet sheet, Long studentId) {
        return publishedAtFor(sheet, studentId) != null;
    }

    private boolean hideEmptyColumnAddedAfterPublish(
        AssessmentSheetColumn column,
        Map<String, BigDecimal> scores,
        LocalDateTime publishedAt) {
        if (publishedAt == null || column.getCreatedAt() == null) {
            return false;
        }
        boolean hasScore = scores.values().stream().anyMatch(Objects::nonNull);
        return !hasScore && column.getCreatedAt().isAfter(publishedAt);
    }

    private AssessmentSheet requirePublishedSheet(
        Student student,
        AssessmentSheetType type,
        Long termId,
        int week) {
        AssessmentSheet sheet = student.getClassId() == null
            ? null
            : assessmentSheetRepository
                .findBySheetTypeAndClassIdAndTermIdAndWeekNumber(type, student.getClassId(), termId, week)
                .filter(candidate -> isPublishedForStudent(candidate, student.getId()))
                .orElse(null);
        if (sheet == null) {
            sheet = publicationRepository.findByStudentId(student.getId()).stream()
                .map(publication -> assessmentSheetRepository.findById(publication.getSheetId()).orElse(null))
                .filter(candidate -> candidate != null
                    && candidate.getSheetType() == type
                    && termId.equals(candidate.getTermId())
                    && candidate.getWeekNumber() == week
                    && !cellRepository.findBySheetIdAndStudentId(candidate.getId(), student.getId()).isEmpty())
                .findFirst()
                .orElse(null);
        }
        if (sheet == null) {
            sheet = assessmentSheetRepository
                .findBySheetTypeAndTermIdAndWeekNumberAndPublishedAtIsNotNull(type, termId, week)
                .stream()
                .filter(candidate -> !cellRepository
                    .findBySheetIdAndStudentId(candidate.getId(), student.getId())
                    .isEmpty())
                .findFirst()
                .orElse(null);
        }
        if (sheet == null || !isPublishedForStudent(sheet, student.getId())) {
            throw new ForbiddenException("This report is not published yet");
        }
        return sheet;
    }

    private StudentAssessmentReportResponse buildAssessment(Student student, AssessmentSheet sheet) {
        Term term = termRepository.findById(sheet.getTermId()).orElse(null);
        AcademicSession session = term != null
            ? academicSessionRepository.findById(term.getSessionId()).orElse(null)
            : academicSessionRepository.findById(sheet.getSessionId()).orElse(null);
        String className = schoolClassRepository.findById(sheet.getClassId())
            .map(SchoolClass::getName)
            .orElse(null);

        List<AssessmentSheetScoreType> types =
            scoreTypeRepository.findBySheetIdOrderBySortOrderAscIdAsc(sheet.getId());
        List<AssessmentSheetColumn> columns =
            columnRepository.findBySheetIdOrderBySortOrderAscIdAsc(sheet.getId());
        Map<Long, Map<String, BigDecimal>> byColumn = new LinkedHashMap<>();
        for (AssessmentSheetCell cell : cellRepository.findBySheetIdAndStudentId(sheet.getId(), student.getId())) {
            byColumn.computeIfAbsent(cell.getColumnId(), id -> new LinkedHashMap<>())
                .put(cell.getTypeCode(), cell.getScore());
        }

        List<StudentAssessmentReportResponse.ScoreType> typeRows = types.stream()
            .map(t -> new StudentAssessmentReportResponse.ScoreType(t.getCode(), t.getName(), t.getMaxScore()))
            .toList();
        LocalDateTime publishedAt = publishedAtFor(sheet, student.getId());
        List<StudentAssessmentReportResponse.SubjectRow> subjects = new ArrayList<>();
        for (AssessmentSheetColumn column : columns) {
            Map<String, BigDecimal> scores = new LinkedHashMap<>();
            Map<String, BigDecimal> stored = byColumn.getOrDefault(column.getId(), Map.of());
            for (AssessmentSheetScoreType type : types) {
                scores.put(type.getCode(), stored.get(type.getCode()));
            }
            if (hideEmptyColumnAddedAfterPublish(column, scores, publishedAt)) {
                continue;
            }
            subjects.add(new StudentAssessmentReportResponse.SubjectRow(column.getName(), scores));
        }

        BigDecimal averageScore = sheet.getSheetType() == AssessmentSheetType.WEEKLY_TEST
            ? percentageAverage(subjects, typeRows)
            : sheet.getSheetType() == AssessmentSheetType.MIDTERM
                ? midtermTotalPercentageAverage(subjects, typeRows)
                : null;

        PupilHeader header = pupilHeader(student, sheet.getClassId(), term, publishedAt);
        return new StudentAssessmentReportResponse(
            student.getId(),
            formatStudentName(student),
            student.getAdmissionNumber(),
            className,
            sheet.getTermId(),
            term != null ? term.getName() : null,
            session != null ? session.getName() : null,
            sheet.getSheetType().name(),
            sheet.getWeekNumber(),
            publishedAt,
            averageScore,
            header.gender(),
            header.ageYears(),
            header.height(),
            header.weight(),
            header.classTeacherName(),
            header.daysSchoolOpened(),
            header.daysPresent(),
            header.reportDate(),
            typeRows,
            subjects);
    }

    private StudentBehaviouralReportView buildBehavioural(Student student, BehaviouralReport report) {
        Term term = termRepository.findById(report.getTermId()).orElse(null);
        AcademicSession session = term != null
            ? academicSessionRepository.findById(term.getSessionId()).orElse(null)
            : academicSessionRepository.findById(report.getSessionId()).orElse(null);
        String className = schoolClassRepository.findById(report.getClassId())
            .map(SchoolClass::getName)
            .orElse(null);
        PupilHeader header = pupilHeader(student, report.getClassId(), term, report.getPublishedAt());
        return new StudentBehaviouralReportView(
            student.getId(),
            formatStudentName(student),
            student.getAdmissionNumber(),
            className,
            report.getTermId(),
            term != null ? term.getName() : null,
            session != null ? session.getName() : null,
            report.getPeriodType().name(),
            report.getWeekNumber(),
            ratingLabel(report.getPunctuality()),
            ratingLabel(report.getNeatness()),
            ratingLabel(report.getCooperation()),
            ratingLabel(report.getResponsibility()),
            ratingLabel(report.getConduct()),
            report.getRemarks(),
            report.getPublishedAt(),
            header.gender(),
            header.ageYears(),
            header.height(),
            header.weight(),
            header.classTeacherName(),
            header.daysSchoolOpened(),
            header.daysPresent(),
            header.reportDate());
    }

    private List<PreschoolReport> publishedPreschoolReportsFor(Student student) {
        Map<Long, PreschoolReport> byId = new LinkedHashMap<>();
        if (student.getClassId() != null) {
            for (PreschoolReport report : preschoolReportRepository
                .findByClassIdAndPublishedAtIsNotNull(student.getClassId())) {
                byId.put(report.getId(), report);
            }
        }
        Set<Long> reportIds = preschoolCommentRepository.findByStudentId(student.getId()).stream()
            .map(PreschoolReportComment::getReportId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        for (PreschoolReport report : preschoolReportRepository.findAllById(reportIds)) {
            if (report.getPublishedAt() != null) {
                byId.put(report.getId(), report);
            }
        }
        return new ArrayList<>(byId.values());
    }

    private PreschoolReport requirePublishedPreschool(Student student, Long termId, int month) {
        if (student.getClassId() != null) {
            PreschoolReport report = preschoolReportRepository
                .findByClassIdAndTermIdAndMonthNumber(student.getClassId(), termId, month)
                .orElse(null);
            if (report != null && report.getPublishedAt() != null) {
                return report;
            }
        }
        for (PreschoolReport report : publishedPreschoolReportsFor(student)) {
            int reportMonth = report.getMonthNumber() == null ? 0 : report.getMonthNumber();
            if (termId.equals(report.getTermId()) && reportMonth == month && report.getPublishedAt() != null) {
                return report;
            }
        }
        throw new ForbiddenException("This report is not published yet");
    }

    private StudentPreschoolReportResponse buildPreschool(Student student, PreschoolReport report) {
        Term term = termRepository.findById(report.getTermId()).orElse(null);
        AcademicSession session = term != null
            ? academicSessionRepository.findById(term.getSessionId()).orElse(null)
            : academicSessionRepository.findById(report.getSessionId()).orElse(null);
        String className = schoolClassRepository.findById(report.getClassId())
            .map(SchoolClass::getName)
            .orElse(null);
        List<StudentPreschoolReportResponse.SubjectComment> subjects = preschoolCommentRepository
            .findByReportIdAndStudentId(report.getId(), student.getId()).stream()
            .sorted(Comparator.comparing(
                c -> c.getSubjectName() != null ? c.getSubjectName() : "",
                String.CASE_INSENSITIVE_ORDER))
            .map(c -> new StudentPreschoolReportResponse.SubjectComment(c.getSubjectName(), c.getComment()))
            .toList();
        PreschoolReportSummary summary = preschoolSummaryRepository
            .findByReportIdAndStudentId(report.getId(), student.getId())
            .orElse(null);
        PupilHeader header = pupilHeader(student, report.getClassId(), term, report.getPublishedAt());
        return new StudentPreschoolReportResponse(
            student.getId(),
            formatStudentName(student),
            student.getAdmissionNumber(),
            className,
            report.getTermId(),
            term != null ? term.getName() : null,
            report.getMonthNumber() == null ? 0 : report.getMonthNumber(),
            session != null ? session.getName() : null,
            report.getPublishedAt(),
            header.gender(),
            header.ageYears(),
            header.height(),
            header.weight(),
            header.classTeacherName(),
            header.daysSchoolOpened(),
            header.daysPresent(),
            header.reportDate(),
            summary != null ? summary.getClassTeacherComment() : null,
            summary != null ? summary.getHeadTeacherRemark() : null,
            subjects);
    }

    private PublishedStudentReportItem toItem(
        String kind,
        Long termId,
        Long sessionId,
        Long classId,
        Integer weekNumber,
        String periodType,
        java.time.LocalDateTime publishedAt,
        Integer monthNumber) {
        Term term = termId != null ? termRepository.findById(termId).orElse(null) : null;
        Long resolvedSessionId = term != null ? term.getSessionId() : sessionId;
        AcademicSession session = resolvedSessionId != null
            ? academicSessionRepository.findById(resolvedSessionId).orElse(null)
            : null;
        String className = classId != null
            ? schoolClassRepository.findById(classId).map(SchoolClass::getName).orElse(null)
            : null;
        return new PublishedStudentReportItem(
            kind,
            termId,
            term != null ? term.getName() : null,
            resolvedSessionId,
            session != null ? session.getName() : null,
            className,
            weekNumber,
            periodType,
            publishedAt,
            monthNumber);
    }

    private static BigDecimal percentageAverage(
        List<StudentAssessmentReportResponse.SubjectRow> subjects,
        List<StudentAssessmentReportResponse.ScoreType> types) {
        List<BigDecimal> percents = new ArrayList<>();
        for (StudentAssessmentReportResponse.SubjectRow subject : subjects) {
            if (subject.scores() == null) {
                continue;
            }
            for (StudentAssessmentReportResponse.ScoreType type : types) {
                BigDecimal score = subject.scores().get(type.code());
                BigDecimal max = type.maxScore();
                if (score != null && max != null && max.compareTo(BigDecimal.ZERO) > 0) {
                    percents.add(score.multiply(new BigDecimal("100")).divide(max, 2, RoundingMode.HALF_UP));
                }
            }
        }
        if (percents.isEmpty()) {
            return null;
        }
        BigDecimal sum = percents.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(percents.size()), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal midtermTotalPercentageAverage(
        List<StudentAssessmentReportResponse.SubjectRow> subjects,
        List<StudentAssessmentReportResponse.ScoreType> types) {
        List<BigDecimal> percents = new ArrayList<>();
        for (StudentAssessmentReportResponse.SubjectRow subject : subjects) {
            BigDecimal percent = subjectTotalPercent(subject, types);
            if (percent != null) {
                percents.add(percent);
            }
        }
        if (percents.isEmpty()) {
            return null;
        }
        BigDecimal sum = percents.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(percents.size()), 2, RoundingMode.HALF_UP);
    }

    static BigDecimal subjectTotal(StudentAssessmentReportResponse.SubjectRow subject,
        List<StudentAssessmentReportResponse.ScoreType> types) {
        if (subject.scores() == null) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        boolean any = false;
        for (StudentAssessmentReportResponse.ScoreType type : types) {
            BigDecimal score = subject.scores().get(type.code());
            if (score != null) {
                total = total.add(score);
                any = true;
            }
        }
        return any ? total : null;
    }

    static BigDecimal subjectTotalPercent(
        StudentAssessmentReportResponse.SubjectRow subject,
        List<StudentAssessmentReportResponse.ScoreType> types) {
        if (subject.scores() == null) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.ZERO;
        for (StudentAssessmentReportResponse.ScoreType type : types) {
            BigDecimal score = subject.scores().get(type.code());
            if (score != null && type.maxScore() != null && type.maxScore().compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(score);
                max = max.add(type.maxScore());
            }
        }
        if (max.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return total.multiply(new BigDecimal("100")).divide(max, 2, RoundingMode.HALF_UP);
    }

    private static int requireWeek(Integer weekNumber) {
        if (weekNumber == null || weekNumber < 1 || weekNumber > 16) {
            throw new ResourceNotFoundException("Week number must be between 1 and 16");
        }
        return weekNumber;
    }

    private static String ratingLabel(BehaviourRating rating) {
        if (rating == null) {
            return null;
        }
        return switch (rating) {
            case EXCELLENT -> "Excellent";
            case VERY_GOOD -> "Very Good";
            case GOOD -> "Good";
            case FAIR -> "Fair";
            case NEEDS_IMPROVEMENT -> "Needs improvement";
        };
    }

    private static String formatStudentName(Student student) {
        String first = student.getFirstName() != null ? student.getFirstName() : "";
        String last = student.getLastName() != null ? student.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isBlank() ? student.getAdmissionNumber() : name;
    }

    private PupilHeader pupilHeader(Student student, Long classId, Term term, LocalDateTime publishedAt) {
        LocalDate reportDay = publishedAt != null
            ? publishedAt.toLocalDate()
            : (term != null && term.getEndDate() != null ? term.getEndDate() : LocalDate.now());
        return new PupilHeader(
            formatGender(student.getGender()),
            StudentAgeUtil.yearsAt(student.getDateOfBirth(), reportDay),
            student.getHeight(),
            student.getWeight(),
            classTeacherName(classId),
            term != null ? daysSchoolOpened(classId, term) : null,
            term != null ? daysPresent(student.getId(), term) : null,
            reportDay.format(REPORT_DATE));
    }

    private String classTeacherName(Long classId) {
        if (classId == null) {
            return null;
        }
        return teacherClassRepository.findByClassIdAndIsActiveTrue(classId).stream()
            .map(TeacherClass::getTeacherId)
            .map(teacherRepository::findById)
            .flatMap(java.util.Optional::stream)
            .map(Teacher::getFullName)
            .filter(name -> name != null && !name.isBlank())
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .reduce((left, right) -> left + " / " + right)
            .orElse(null);
    }

    private Integer daysSchoolOpened(Long classId, Term term) {
        if (classId == null) {
            return null;
        }
        if (term.getId() != null) {
            long byTerm = attendanceRepository.countDistinctDatesByClassIdAndTermId(classId, term.getId());
            if (byTerm > 0) {
                return (int) byTerm;
            }
        }
        if (term.getStartDate() != null && term.getEndDate() != null) {
            return (int) attendanceRepository.countDistinctDatesByClassIdAndDateBetween(
                classId, term.getStartDate(), term.getEndDate());
        }
        return null;
    }

    private Integer daysPresent(Long studentId, Term term) {
        if (term.getId() != null) {
            long byTerm = attendanceRepository.countByStudentIdAndTermIdAndStatus(
                studentId, term.getId(), AttendanceStatus.PRESENT);
            if (byTerm > 0 || term.getStartDate() == null || term.getEndDate() == null) {
                return (int) byTerm;
            }
        }
        if (term.getStartDate() != null && term.getEndDate() != null) {
            return (int) attendanceRepository.findByStudentIdAndDateBetween(
                    studentId, term.getStartDate(), term.getEndDate()).stream()
                .filter(row -> row.getStatus() == AttendanceStatus.PRESENT)
                .count();
        }
        return null;
    }

    private static String formatGender(Gender gender) {
        if (gender == null) {
            return null;
        }
        String name = gender.name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private record PupilHeader(
        String gender,
        Integer ageYears,
        String height,
        String weight,
        String classTeacherName,
        Integer daysSchoolOpened,
        Integer daysPresent,
        String reportDate
    ) {
    }
}
