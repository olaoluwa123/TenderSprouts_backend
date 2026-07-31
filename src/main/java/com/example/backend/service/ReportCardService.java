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
import com.example.backend.entity.Term;
import com.example.backend.entity.TermResult;
import com.example.backend.entity.User;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AcademicSessionRepository;
import com.example.backend.repository.AssessmentTypeLookupRepository;
import com.example.backend.repository.GradeRepository;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.repository.TermResultRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.security.SecurityUser;
import com.example.backend.util.GradeScoringUtil;
import com.example.backend.util.LetterGradeUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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
    private final AssessmentTypeLookupRepository assessmentTypeLookupRepository;
    private final ReportCardPdfGenerator pdfGenerator;

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
        this.assessmentTypeLookupRepository = assessmentTypeLookupRepository;
        this.pdfGenerator = pdfGenerator;
    }

    public ReportCardResponse getReportCard(Long studentId, Long termId, SecurityUser actor) {
        TermResult result = termResultRepository.findByStudentIdAndTermId(studentId, termId)
            .orElseThrow(() -> new ResourceNotFoundException("Term result not found"));
        assertCanView(result, actor);
        return buildReportCard(result);
    }

    public byte[] generatePdf(Long studentId, Long termId, SecurityUser actor) {
        return pdfGenerator.generate(getReportCard(studentId, termId, actor));
    }

    private ReportCardResponse buildReportCard(TermResult result) {
        Student student = studentRepository.findById(result.getStudentId()).orElseThrow();
        Term term = termRepository.findById(result.getTermId()).orElseThrow();
        AcademicSession session = academicSessionRepository.findById(result.getSessionId()).orElseThrow();
        String className = schoolClassRepository.findById(result.getClassId())
            .map(SchoolClass::getName).orElse(null);

        List<AssessmentTypeLookup> weightedTypes = assessmentTypeLookupRepository
            .findByIsActiveTrueOrderBySortOrderAscCodeAsc().stream()
            .filter(type -> type.getWeight() != null && type.getWeight().compareTo(BigDecimal.ZERO) > 0)
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
                    breakdown));
            }
        }

        String letterGrade = LetterGradeUtil.toLetter(result.getAverageScore());

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
            subjectGrades);
    }

    private void assertCanView(TermResult result, SecurityUser actor) {
        if (actor.getRole() == User.Role.ADMIN) {
            return;
        }
        if (result.getPublishedAt() == null) {
            throw new ForbiddenException("Report card not available until term results are published");
        }
        if (actor.getRole() == User.Role.TEACHER) {
            if (!teacherClassRepository.existsByTeacherIdAndClassIdAndIsActiveTrue(
                actor.getProfileId(), result.getClassId())) {
                throw new ForbiddenException("Not allowed to view this report card");
            }
            return;
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

    private static String formatStudentName(Student student) {
        String first = student.getFirstName() != null ? student.getFirstName() : "";
        String last = student.getLastName() != null ? student.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isBlank() ? student.getAdmissionNumber() : name;
    }
}
