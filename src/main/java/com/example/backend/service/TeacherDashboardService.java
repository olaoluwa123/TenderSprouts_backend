package com.example.backend.service;

import com.example.backend.dto.dashboard.GradeDistributionBucket;
import com.example.backend.dto.dashboard.GradingProgress;
import com.example.backend.dto.dashboard.StudentPerformanceItem;
import com.example.backend.dto.dashboard.TeacherDashboardResponse;
import com.example.backend.dto.dashboard.TeacherDashboardResponse.AssignedClass;
import com.example.backend.dto.dashboard.UpcomingExamItem;
import com.example.backend.entity.AcademicSession;
import com.example.backend.entity.ExamTimetableSlot;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Student;
import com.example.backend.entity.TeacherClass;
import com.example.backend.entity.Term;
import com.example.backend.entity.TermResult;
import com.example.backend.entity.TermResultSubmission;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.EnrollmentStatus;
import com.example.backend.entity.enums.TermResultSubmissionStatus;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.repository.ClassSubjectRepository;
import com.example.backend.repository.ExamTimetableSlotRepository;
import com.example.backend.repository.GradeRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentEnrollmentRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TermResultRepository;
import com.example.backend.repository.TermResultSubmissionRepository;
import com.example.backend.security.SecurityUser;
import com.example.backend.util.LetterGradeUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TeacherDashboardService {

    private final ActiveSessionService activeSessionService;
    private final ActiveTermService activeTermService;
    private final TeacherClassRepository teacherClassRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final GradeRepository gradeRepository;
    private final TermResultRepository termResultRepository;
    private final TermResultSubmissionRepository termResultSubmissionRepository;
    private final StudentRepository studentRepository;
    private final ExamTimetableSlotRepository examTimetableSlotRepository;
    private final SubjectRepository subjectRepository;

    public TeacherDashboardService(
        ActiveSessionService activeSessionService,
        ActiveTermService activeTermService,
        TeacherClassRepository teacherClassRepository,
        SchoolClassRepository schoolClassRepository,
        StudentEnrollmentRepository studentEnrollmentRepository,
        ClassSubjectRepository classSubjectRepository,
        GradeRepository gradeRepository,
        TermResultRepository termResultRepository,
        TermResultSubmissionRepository termResultSubmissionRepository,
        StudentRepository studentRepository,
        ExamTimetableSlotRepository examTimetableSlotRepository,
        SubjectRepository subjectRepository) {
        this.activeSessionService = activeSessionService;
        this.activeTermService = activeTermService;
        this.teacherClassRepository = teacherClassRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.gradeRepository = gradeRepository;
        this.termResultRepository = termResultRepository;
        this.termResultSubmissionRepository = termResultSubmissionRepository;
        this.studentRepository = studentRepository;
        this.examTimetableSlotRepository = examTimetableSlotRepository;
        this.subjectRepository = subjectRepository;
    }

    public TeacherDashboardResponse getDashboard(SecurityUser actor) {
        return getDashboard(actor, null);
    }

    public TeacherDashboardResponse getDashboard(SecurityUser actor, Long requestedClassId) {
        if (actor.getRole() != User.Role.TEACHER && actor.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Teacher access required");
        }
        if (actor.getRole() == User.Role.TEACHER && actor.getProfileId() == null) {
            throw new ForbiddenException("Teacher profile required");
        }

        AcademicSession session = activeSessionService.requireActiveSession();
        Term term = activeTermService.requireActiveTerm();
        Long sessionId = session.getId();
        Long termId = term.getId();

        List<TeacherClass> assignments = actor.getRole() == User.Role.TEACHER
            ? teacherClassRepository.findByTeacherIdAndIsActiveTrue(actor.getProfileId())
            : List.of();
        List<AssignedClass> assignedClasses = assignments.stream()
            .map(row -> new AssignedClass(
                row.getClassId(),
                schoolClassRepository.findById(row.getClassId())
                    .map(SchoolClass::getName)
                    .orElse("Class #" + row.getClassId())))
            .toList();

        if (assignedClasses.isEmpty()) {
            return emptyTeacherDashboard(session, term);
        }

        AssignedClass selected = assignedClasses.stream()
            .filter(row -> requestedClassId == null || row.classId().equals(requestedClassId))
            .findFirst()
            .orElseThrow(() -> new ForbiddenException("Not assigned to this class"));
        Long classId = selected.classId();
        String className = selected.className();

        long studentCount = studentEnrollmentRepository.countByClassIdAndSessionIdAndStatus(
            classId, sessionId, EnrollmentStatus.ACTIVE);
        long studentsWithGrades = gradeRepository.countDistinctStudentsWithGrades(classId, termId);
        long subjectCount = classSubjectRepository.countByClassId(classId);

        List<TermResult> results = termResultRepository.findByClassIdAndTermIdOrderByAverageScoreDesc(classId, termId);
        BigDecimal classAverage = computeClassAverage(results);
        TermResultSubmissionStatus submissionStatus = termResultSubmissionRepository
            .findByClassIdAndTermId(classId, termId)
            .map(TermResultSubmission::getStatus)
            .orElse(null);

        String nextAction = resolveNextAction(studentCount, studentsWithGrades, results.size(), submissionStatus);
        int percent = studentCount == 0 ? 0
            : (int) Math.min(100, Math.round((studentsWithGrades * 100.0) / studentCount));

        GradingProgress progress = new GradingProgress(
            studentCount, studentsWithGrades, subjectCount, percent, nextAction);

        return new TeacherDashboardResponse(
            sessionId,
            session.getName(),
            termId,
            term.getName(),
            classId,
            className,
            assignedClasses,
            studentCount,
            classAverage,
            submissionStatus != null ? submissionStatus.name() : "NOT_STARTED",
            progress,
            buildGradeDistribution(results),
            buildPerformers(results, true, 5),
            buildPerformers(results, false, 5),
            buildUpcomingExams(classId, termId));
    }

    private TeacherDashboardResponse emptyTeacherDashboard(AcademicSession session, Term term) {
        return new TeacherDashboardResponse(
            session.getId(),
            session.getName(),
            term.getId(),
            term.getName(),
            null,
            null,
            List.of(),
            0,
            null,
            "NOT_STARTED",
            new GradingProgress(0, 0, 0, 0, "Ask an admin to assign you to a class"),
            List.of(),
            List.of(),
            List.of(),
            List.of());
    }

    private static BigDecimal computeClassAverage(List<TermResult> results) {
        List<BigDecimal> scores = results.stream()
            .map(TermResult::getAverageScore)
            .filter(score -> score != null)
            .toList();
        if (scores.isEmpty()) {
            return null;
        }
        BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    private static String resolveNextAction(
        long enrolled,
        long withGrades,
        int resultCount,
        TermResultSubmissionStatus status) {
        if (status == TermResultSubmissionStatus.PUBLISHED) {
            return "Results published — parents can view report cards";
        }
        if (status == TermResultSubmissionStatus.SUBMITTED) {
            return "Waiting for admin approval";
        }
        if (enrolled == 0) {
            return "No students enrolled in your class yet";
        }
        if (withGrades < enrolled) {
            return "Enter grades for remaining students";
        }
        if (resultCount == 0) {
            return "Compute term results, then submit to admin";
        }
        return "Submit term results to admin for approval";
    }

    private List<GradeDistributionBucket> buildGradeDistribution(List<TermResult> results) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String letter : Arrays.asList("A", "B", "C", "D", "E", "F")) {
            counts.put(letter, 0L);
        }
        for (TermResult result : results) {
            String letter = LetterGradeUtil.toLetter(result.getAverageScore());
            if (letter != null) {
                counts.merge(letter, 1L, Long::sum);
            }
        }
        return counts.entrySet().stream()
            .map(e -> new GradeDistributionBucket(e.getKey(), e.getValue()))
            .toList();
    }

    private List<StudentPerformanceItem> buildPerformers(List<TermResult> results, boolean top, int limit) {
        List<TermResult> ordered = new ArrayList<>(results.stream()
            .filter(r -> r.getAverageScore() != null)
            .toList());
        if (!top) {
            ordered.sort((a, b) -> a.getAverageScore().compareTo(b.getAverageScore()));
        }
        return ordered.stream()
            .limit(limit)
            .map(this::toPerformanceItem)
            .toList();
    }

    private StudentPerformanceItem toPerformanceItem(TermResult result) {
        Student student = studentRepository.findById(result.getStudentId()).orElse(null);
        String name = student != null ? formatStudentName(student) : "Student #" + result.getStudentId();
        String admission = student != null ? student.getAdmissionNumber() : null;
        return new StudentPerformanceItem(
            result.getStudentId(),
            name,
            admission,
            result.getAverageScore(),
            result.getRankInClass(),
            LetterGradeUtil.toLetter(result.getAverageScore()));
    }

    private List<UpcomingExamItem> buildUpcomingExams(Long classId, Long termId) {
        LocalDate today = LocalDate.now();
        return examTimetableSlotRepository
            .findByClassIdAndTermIdOrderByExamDateAscStartTimeAsc(classId, termId)
            .stream()
            .filter(slot -> !slot.getExamDate().isBefore(today))
            .limit(5)
            .map(this::toUpcomingExam)
            .toList();
    }

    private UpcomingExamItem toUpcomingExam(ExamTimetableSlot slot) {
        String subjectName = subjectRepository.findById(slot.getSubjectId())
            .map(s -> s.getName())
            .orElse("Subject #" + slot.getSubjectId());
        return new UpcomingExamItem(
            slot.getId(),
            subjectName,
            slot.getExamDate(),
            slot.getStartTime(),
            slot.getEndTime(),
            slot.getRoom());
    }

    private static String formatStudentName(Student student) {
        String first = student.getFirstName() != null ? student.getFirstName() : "";
        String last = student.getLastName() != null ? student.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isBlank() ? student.getAdmissionNumber() : name;
    }
}
