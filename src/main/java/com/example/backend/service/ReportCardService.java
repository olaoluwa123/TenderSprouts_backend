package com.example.backend.service;

import com.example.backend.dto.reportcard.ReportCardResponse;
import com.example.backend.dto.reportcard.ReportCardSubjectGrade;
import com.example.backend.dto.reportcard.ReportCardSubjectSummary;
import com.example.backend.entity.AcademicSession;
import com.example.backend.entity.AssessmentTypeLookup;
import com.example.backend.entity.Grade;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Student;
import com.example.backend.entity.Subject;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.TeacherClass;
import com.example.backend.entity.Term;
import com.example.backend.entity.TermResult;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.AttendanceStatus;
import com.example.backend.entity.enums.Gender;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AcademicSessionRepository;
import com.example.backend.repository.AssessmentTypeLookupRepository;
import com.example.backend.repository.AttendanceRepository;
import com.example.backend.repository.GradeRepository;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.repository.TermResultRepository;
import com.example.backend.security.SecurityUser;
import com.example.backend.util.GradeScoringUtil;
import com.example.backend.util.LetterGradeUtil;
import com.example.backend.util.PerformanceBandUtil;
import com.example.backend.util.StudentAgeUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ReportCardService {

    private final TermResultRepository termResultRepository;
    private final GradeRepository gradeRepository;
    private final StudentRepository studentRepository;
    private final TermRepository termRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final TeacherRepository teacherRepository;
    private final AttendanceRepository attendanceRepository;
    private final AssessmentTypeLookupRepository assessmentTypeLookupRepository;
    private final ReportCardPdfGenerator pdfGenerator;

    private static final DateTimeFormatter REPORT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ReportCardService(
        TermResultRepository termResultRepository,
        GradeRepository gradeRepository,
        StudentRepository studentRepository,
        TermRepository termRepository,
        AcademicSessionRepository academicSessionRepository,
        SchoolClassRepository schoolClassRepository,
        SubjectRepository subjectRepository,
        ParentStudentRepository parentStudentRepository,
        TeacherClassRepository teacherClassRepository,
        TeacherRepository teacherRepository,
        AttendanceRepository attendanceRepository,
        AssessmentTypeLookupRepository assessmentTypeLookupRepository,
        ReportCardPdfGenerator pdfGenerator) {
        this.termResultRepository = termResultRepository;
        this.gradeRepository = gradeRepository;
        this.studentRepository = studentRepository;
        this.termRepository = termRepository;
        this.academicSessionRepository = academicSessionRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
        this.parentStudentRepository = parentStudentRepository;
        this.teacherClassRepository = teacherClassRepository;
        this.teacherRepository = teacherRepository;
        this.attendanceRepository = attendanceRepository;
        this.assessmentTypeLookupRepository = assessmentTypeLookupRepository;
        this.pdfGenerator = pdfGenerator;
    }

    public ReportCardResponse getReportCard(Long studentId, Long termId, SecurityUser actor) {
        return termResultRepository.findByStudentIdAndTermId(studentId, termId)
            .map(result -> {
                assertCanView(result, actor);
                return buildReportCard(result);
            })
            .orElseGet(() -> previewFromSavedGrades(studentId, termId, actor));
    }

    public byte[] generatePdf(Long studentId, Long termId, SecurityUser actor) {
        return pdfGenerator.generate(getReportCard(studentId, termId, actor));
    }

    /**
     * Builds a PDF after results have been published (no actor / parent ACL check).
     */
    public byte[] generatePdfForPublished(Long studentId, Long termId) {
        TermResult result = termResultRepository.findByStudentIdAndTermId(studentId, termId)
            .orElseThrow(() -> new ResourceNotFoundException("Term result not found"));
        if (result.getPublishedAt() == null) {
            throw new ForbiddenException("Report card is not published yet");
        }
        return pdfGenerator.generate(buildReportCard(result));
    }

    private ReportCardResponse buildReportCard(TermResult result) {
        Student student = studentRepository.findById(result.getStudentId()).orElseThrow();
        Term term = termRepository.findById(result.getTermId()).orElseThrow();
        AcademicSession session = academicSessionRepository.findById(result.getSessionId()).orElseThrow();
        String className = schoolClassRepository.findById(result.getClassId())
            .map(SchoolClass::getName).orElse(null);

        List<AssessmentTypeLookup> weightedTypes = assessmentTypeLookupRepository
            .findByIsActiveTrueOrderBySortOrderAscCodeAsc().stream()
            .filter(type -> type.getDefaultMaxScore() != null && type.getDefaultMaxScore().signum() > 0)
            .toList();
        Set<String> weightedTypeCodes = GradeScoringUtil.weightedTypeCodes(weightedTypes);

        List<Grade> grades = gradeRepository.findByStudentIdAndTermId(student.getId(), term.getId());
        Map<Long, List<Grade>> gradesBySubject = grades.stream()
            .collect(Collectors.groupingBy(Grade::getSubjectId));

        List<ReportCardSubjectGrade> subjectGrades = new ArrayList<>();
        List<ReportCardSubjectSummary> subjectSummaries = new ArrayList<>();

        for (Map.Entry<Long, List<Grade>> entry : gradesBySubject.entrySet()) {
            Long subjectId = entry.getKey();
            List<Grade> subjectGradeList = entry.getValue();
            Subject subject = subjectRepository.findById(subjectId).orElse(null);
            String subjectName = subject != null ? subject.getName() : null;

            List<ReportCardSubjectGrade> breakdown = new ArrayList<>();
            for (Grade grade : subjectGradeList) {
                if (!weightedTypeCodes.contains(grade.getAssessmentType())) {
                    continue;
                }
                BigDecimal pct = null;
                if (grade.getScore() != null && grade.getMaxScore() != null
                    && grade.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
                    pct = grade.getScore()
                        .divide(grade.getMaxScore(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP);
                }
                ReportCardSubjectGrade row = new ReportCardSubjectGrade(
                    grade.getSubjectId(),
                    subjectName,
                    grade.getAssessmentType(),
                    grade.getScore(),
                    grade.getMaxScore(),
                    pct);
                breakdown.add(row);
                subjectGrades.add(row);
            }

            BigDecimal subjectTotal = GradeScoringUtil.computeSubjectTotal(subjectGradeList, weightedTypeCodes);
            if (subjectTotal != null) {
                subjectSummaries.add(new ReportCardSubjectSummary(
                    subjectId,
                    subjectName,
                    subjectTotal,
                    LetterGradeUtil.toLetter(subjectTotal),
                    breakdown,
                    PerformanceBandUtil.toBand(subjectTotal)));
            }
        }

        String letterGrade = LetterGradeUtil.toLetter(result.getAverageScore());
        LocalDate reportDay = result.getPublishedAt() != null
            ? result.getPublishedAt().toLocalDate()
            : (term.getEndDate() != null ? term.getEndDate() : LocalDate.now());
        Integer ageYears = StudentAgeUtil.yearsAt(student.getDateOfBirth(), reportDay);
        BigDecimal totalScore = subjectSummaries.stream()
            .map(ReportCardSubjectSummary::subjectTotal)
            .filter(score -> score != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        subjectSummaries.sort(Comparator.comparing(
            s -> s.subjectName() != null ? s.subjectName() : "",
            String.CASE_INSENSITIVE_ORDER));

        String performanceBand = PerformanceBandUtil.toBand(result.getAverageScore());
        String attitudeComment = attitudeComment(performanceBand);
        String areasToImprove = areasToImprove(subjectSummaries);

        return new ReportCardResponse(
            student.getId(),
            formatStudentName(student),
            student.getAdmissionNumber(),
            term.getId(),
            term.getName(),
            session.getId(),
            session.getName(),
            result.getClassId(),
            className,
            result.getAverageScore(),
            letterGrade,
            result.getRankInClass(),
            result.getComments(),
            subjectSummaries,
            subjectGrades,
            formatGender(student.getGender()),
            ageYears,
            classTeacherName(result.getClassId()),
            daysSchoolOpened(result.getClassId(), term),
            daysPresent(student.getId(), term),
            reportDay.format(REPORT_DATE),
            nextTermBegins(term),
            subjectSummaries.isEmpty() ? null : totalScore,
            performanceBand,
            student.getHeight(),
            student.getWeight(),
            attitudeComment,
            areasToImprove);
    }

    private static String attitudeComment(String band) {
        if (PerformanceBandUtil.EXCELLENT.equals(band) || PerformanceBandUtil.VERY_GOOD.equals(band)) {
            return "An encouraging result! Keep the flag flying.";
        }
        if (PerformanceBandUtil.GOOD.equals(band) || PerformanceBandUtil.SATISFACTORY.equals(band)) {
            return "A fair effort. More consistency will yield a stronger result.";
        }
        if (band != null) {
            return "Needs closer attention and support next term.";
        }
        return "";
    }

    private static String areasToImprove(List<ReportCardSubjectSummary> summaries) {
        List<String> weak = summaries.stream()
            .filter(row -> row.subjectTotal() != null && row.subjectTotal().compareTo(new BigDecimal("70")) < 0)
            .map(ReportCardSubjectSummary::subjectName)
            .filter(name -> name != null && !name.isBlank())
            .toList();
        if (weak.isEmpty()) {
            return "Keep up the good work in all subjects.";
        }
        return "He/She needs to improve on " + String.join(", ", weak) + ".";
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

    private String nextTermBegins(Term current) {
        List<Term> terms = termRepository.findBySessionIdOrderByNameAsc(current.getSessionId());
        terms.sort(Comparator.comparing(Term::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())));
        for (Term term : terms) {
            if (current.getStartDate() != null && term.getStartDate() != null
                && term.getStartDate().isAfter(current.getStartDate())) {
                return term.getStartDate().format(REPORT_DATE);
            }
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

    private void assertCanView(TermResult result, SecurityUser actor) {
        if (actor.getRole() == User.Role.ADMIN) {
            return;
        }
        if (actor.getRole() == User.Role.TEACHER) {
            if (!teacherClassRepository.existsByTeacherIdAndClassIdAndIsActiveTrue(
                actor.getProfileId(), result.getClassId())) {
                throw new ForbiddenException("Not allowed to view this report card");
            }
            return;
        }
        if (result.getPublishedAt() == null) {
            throw new ForbiddenException("Report card not available until term results are published");
        }
        if (actor.getRole() == User.Role.PARENT) {
            if (!parentStudentRepository.existsByParentIdAndStudentId(
                actor.getProfileId(), result.getStudentId())) {
                throw new ForbiddenException("Not allowed to view this report card");
            }
            return;
        }
        throw new ForbiddenException("Not allowed to view this report card");
    }

    private ReportCardResponse previewFromSavedGrades(Long studentId, Long termId, SecurityUser actor) {
        if (actor.getRole() == User.Role.PARENT) {
            throw new ForbiddenException("Report card not available until term results are published");
        }
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        Term term = termRepository.findById(termId)
            .orElseThrow(() -> new ResourceNotFoundException("Term not found"));
        List<Grade> grades = gradeRepository.findByStudentIdAndTermId(studentId, termId);
        if (grades.isEmpty()) {
            throw new ResourceNotFoundException("Save grades before viewing the report");
        }
        Long classId = student.getClassId() != null ? student.getClassId() : grades.get(0).getClassId();
        if (actor.getRole() == User.Role.TEACHER
            && !teacherClassRepository.existsByTeacherIdAndClassIdAndIsActiveTrue(actor.getProfileId(), classId)) {
            throw new ForbiddenException("Not allowed to view this report card");
        }
        if (actor.getRole() != User.Role.ADMIN && actor.getRole() != User.Role.TEACHER) {
            throw new ForbiddenException("Not allowed to view this report card");
        }
        List<AssessmentTypeLookup> weightedTypes = assessmentTypeLookupRepository
            .findByIsActiveTrueOrderBySortOrderAscCodeAsc().stream()
            .filter(type -> type.getDefaultMaxScore() != null && type.getDefaultMaxScore().signum() > 0)
            .toList();
        TermResult draft = new TermResult();
        draft.setStudentId(studentId);
        draft.setTermId(termId);
        draft.setSessionId(term.getSessionId());
        draft.setClassId(classId);
        draft.setAverageScore(GradeScoringUtil.computeTermAverage(
            grades, GradeScoringUtil.weightedTypeCodes(weightedTypes)));
        return buildReportCard(draft);
    }

    private static String formatStudentName(Student student) {
        String first = student.getFirstName() != null ? student.getFirstName() : "";
        String last = student.getLastName() != null ? student.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isBlank() ? student.getAdmissionNumber() : name;
    }
}
