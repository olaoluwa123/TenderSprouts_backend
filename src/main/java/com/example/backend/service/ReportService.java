package com.example.backend.service;

import com.example.backend.dto.report.ReportDataResponse;
import com.example.backend.entity.AdmissionApplication;
import com.example.backend.entity.Attendance;
import com.example.backend.entity.Grade;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Student;
import com.example.backend.entity.Subject;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.TermResult;
import com.example.backend.entity.enums.AdmissionApplicationStatus;
import com.example.backend.entity.enums.AttendanceStatus;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AdmissionApplicationRepository;
import com.example.backend.repository.AttendanceRepository;
import com.example.backend.repository.GradeRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TermResultRepository;
import com.example.backend.util.CsvUtf8Writer;
import com.example.backend.util.ReportPdfGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final TermResultRepository termResultRepository;
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final GradeRepository gradeRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final ActiveTermService activeTermService;
    private final ReportPdfGenerator reportPdfGenerator;

    public ReportService(
        TermResultRepository termResultRepository,
        AttendanceRepository attendanceRepository,
        StudentRepository studentRepository,
        TeacherRepository teacherRepository,
        SchoolClassRepository schoolClassRepository,
        AdmissionApplicationRepository admissionApplicationRepository,
        GradeRepository gradeRepository,
        SubjectRepository subjectRepository,
        TeacherClassRepository teacherClassRepository,
        ActiveTermService activeTermService,
        ReportPdfGenerator reportPdfGenerator) {
        this.termResultRepository = termResultRepository;
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.gradeRepository = gradeRepository;
        this.subjectRepository = subjectRepository;
        this.teacherClassRepository = teacherClassRepository;
        this.activeTermService = activeTermService;
        this.reportPdfGenerator = reportPdfGenerator;
    }

    public ReportDataResponse getReport(
        String key,
        Long classId,
        Long subjectId,
        Long studentId,
        Long termId,
        Long teacherId,
        LocalDate date,
        LocalDate from,
        LocalDate to) {
        Long resolvedTermId = termId != null ? termId : tryActiveTermId();
        LocalDate resolvedDate = date != null ? date : LocalDate.now();
        LocalDate resolvedFrom = from != null ? from : resolvedDate.withDayOfMonth(1);
        LocalDate resolvedTo = to != null ? to : resolvedDate;

        return switch (key) {
            case "class-performance" -> classPerformance(resolvedTermId);
            case "subject-performance" -> subjectPerformance(resolvedTermId, classId, subjectId);
            case "pupil-performance" -> pupilPerformance(resolvedTermId, classId, studentId);
            case "term-results" -> termResults(resolvedTermId, classId);
            case "daily-attendance" -> dailyAttendance(resolvedDate, classId);
            case "monthly-attendance" -> monthlyAttendance(resolvedFrom, resolvedTo, classId);
            case "class-attendance" -> classAttendance(resolvedFrom, resolvedTo, classId);
            case "individual-attendance" -> individualAttendance(studentId, resolvedFrom, resolvedTo);
            case "pupil-population" -> pupilPopulation();
            case "teacher-stats" -> teacherStats();
            case "class-stats" -> classStats(resolvedTermId);
            case "admission-stats" -> admissionStats();
            default -> throw new ResourceNotFoundException("Unknown report key: " + key);
        };
    }

    public byte[] export(
        String key,
        String format,
        Long classId,
        Long subjectId,
        Long studentId,
        Long termId,
        Long teacherId,
        LocalDate date,
        LocalDate from,
        LocalDate to) {
        ReportDataResponse report = getReport(
            key, classId, subjectId, studentId, termId, teacherId, date, from, to);
        String normalized = format == null ? "csv" : format.trim().toLowerCase();
        return switch (normalized) {
            case "pdf" -> reportPdfGenerator.generate(report);
            case "csv" -> CsvUtf8Writer.write(report.columns(), report.rows());
            default -> throw new ResourceNotFoundException("Unsupported export format: " + format);
        };
    }

    private ReportDataResponse classPerformance(Long termId) {
        List<String> columns = List.of("Class", "Pupils", "Average Score");
        List<List<Object>> rows = new ArrayList<>();
        if (termId == null) {
            return new ReportDataResponse("Class Performance", columns, rows);
        }
        Map<Long, String> classNames = classNameMap();
        for (Object[] row : termResultRepository.averageScoreByClassForTerm(termId)) {
            Long cid = (Long) row[0];
            Double avg = row[1] instanceof BigDecimal bd
                ? bd.doubleValue()
                : row[1] instanceof Number n ? n.doubleValue() : null;
            long pupils = termResultRepository.findByClassIdAndTermIdOrderByAverageScoreDesc(cid, termId).size();
            rows.add(List.of(
                classNames.getOrDefault(cid, "Class #" + cid),
                pupils,
                avg == null ? "" : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)));
        }
        return new ReportDataResponse("Class Performance", columns, rows);
    }

    private ReportDataResponse subjectPerformance(Long termId, Long classId, Long subjectId) {
        List<String> columns = List.of("Subject", "Class", "Assessments", "Average Score");
        List<List<Object>> rows = new ArrayList<>();
        if (termId == null) {
            return new ReportDataResponse("Subject Performance", columns, rows);
        }
        List<Grade> grades;
        if (classId != null && subjectId != null) {
            grades = gradeRepository.findByClassIdAndTermIdAndSubjectId(classId, termId, subjectId);
        } else if (classId != null) {
            grades = gradeRepository.findByClassIdAndTermId(classId, termId);
        } else {
            grades = gradeRepository.findAll().stream()
                .filter(g -> Objects.equals(g.getTermId(), termId))
                .toList();
        }
        Map<Long, String> subjects = subjectRepository.findAll().stream()
            .collect(Collectors.toMap(Subject::getId, Subject::getName, (a, b) -> a));
        Map<Long, String> classNames = classNameMap();
        Map<String, List<Grade>> grouped = grades.stream()
            .collect(Collectors.groupingBy(g -> g.getSubjectId() + ":" + g.getClassId()));
        for (Map.Entry<String, List<Grade>> entry : grouped.entrySet()) {
            List<Grade> group = entry.getValue();
            Grade sample = group.get(0);
            BigDecimal avg = averageScore(group);
            rows.add(List.of(
                subjects.getOrDefault(sample.getSubjectId(), "Subject #" + sample.getSubjectId()),
                classNames.getOrDefault(sample.getClassId(), "Class #" + sample.getClassId()),
                group.size(),
                avg));
        }
        return new ReportDataResponse("Subject Performance", columns, rows);
    }

    private ReportDataResponse pupilPerformance(Long termId, Long classId, Long studentId) {
        List<String> columns = List.of("Pupil", "Admission No", "Class", "Average", "Rank");
        List<List<Object>> rows = new ArrayList<>();
        if (termId == null) {
            return new ReportDataResponse("Pupil Performance", columns, rows);
        }
        List<TermResult> results;
        if (studentId != null) {
            results = termResultRepository.findByStudentIdAndTermId(studentId, termId)
                .map(List::of)
                .orElse(List.of());
        } else if (classId != null) {
            results = termResultRepository.findByClassIdAndTermIdOrderByAverageScoreDesc(classId, termId);
        } else {
            results = termResultRepository.findByTermId(termId);
        }
        Map<Long, Student> students = studentMap(results.stream().map(TermResult::getStudentId).toList());
        Map<Long, String> classNames = classNameMap();
        for (TermResult result : results) {
            Student student = students.get(result.getStudentId());
            rows.add(List.of(
                studentName(student),
                student != null ? student.getAdmissionNumber() : "",
                classNames.getOrDefault(result.getClassId(), "Class #" + result.getClassId()),
                result.getAverageScore() != null ? result.getAverageScore() : "",
                result.getRankInClass() != null ? result.getRankInClass() : ""));
        }
        return new ReportDataResponse("Pupil Performance", columns, rows);
    }

    private ReportDataResponse termResults(Long termId, Long classId) {
        List<String> columns = List.of("Pupil", "Class", "Average", "Rank", "Published");
        List<List<Object>> rows = new ArrayList<>();
        if (termId == null) {
            return new ReportDataResponse("Term Results", columns, rows);
        }
        List<TermResult> results = classId != null
            ? termResultRepository.findByClassIdAndTermIdOrderByAverageScoreDesc(classId, termId)
            : termResultRepository.findByTermId(termId);
        Map<Long, Student> students = studentMap(results.stream().map(TermResult::getStudentId).toList());
        Map<Long, String> classNames = classNameMap();
        for (TermResult result : results) {
            Student student = students.get(result.getStudentId());
            rows.add(List.of(
                studentName(student),
                classNames.getOrDefault(result.getClassId(), "Class #" + result.getClassId()),
                result.getAverageScore() != null ? result.getAverageScore() : "",
                result.getRankInClass() != null ? result.getRankInClass() : "",
                result.getPublishedAt() != null ? result.getPublishedAt().toString() : ""));
        }
        return new ReportDataResponse("Term Results", columns, rows);
    }

    private ReportDataResponse dailyAttendance(LocalDate date, Long classId) {
        List<String> columns = List.of("Pupil", "Class", "Status");
        List<Attendance> rowsData = classId != null
            ? attendanceRepository.findByClassIdAndDate(classId, date)
            : attendanceRepository.findByDate(date);
        return attendanceReport("Daily Attendance", columns, rowsData);
    }

    private ReportDataResponse monthlyAttendance(LocalDate from, LocalDate to, Long classId) {
        List<String> columns = List.of("Date", "Present", "Absent", "Late", "Excused", "Total");
        List<Attendance> data = classId != null
            ? attendanceRepository.findByClassIdAndDateBetween(classId, from, to)
            : attendanceRepository.findByDateBetween(from, to);
        Map<LocalDate, Map<AttendanceStatus, Long>> byDate = new HashMap<>();
        for (Attendance row : data) {
            byDate.computeIfAbsent(row.getDate(), d -> new EnumMap<>(AttendanceStatus.class))
                .merge(row.getStatus(), 1L, Long::sum);
        }
        List<List<Object>> rows = new ArrayList<>();
        byDate.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            Map<AttendanceStatus, Long> counts = entry.getValue();
            long total = counts.values().stream().mapToLong(Long::longValue).sum();
            rows.add(List.of(
                entry.getKey().toString(),
                counts.getOrDefault(AttendanceStatus.PRESENT, 0L),
                counts.getOrDefault(AttendanceStatus.ABSENT, 0L),
                counts.getOrDefault(AttendanceStatus.LATE, 0L),
                counts.getOrDefault(AttendanceStatus.EXCUSED, 0L),
                total));
        });
        String title = "Monthly Attendance (" + YearMonth.from(from) + ")";
        return new ReportDataResponse(title, columns, rows);
    }

    private ReportDataResponse classAttendance(LocalDate from, LocalDate to, Long classId) {
        if (classId == null) {
            return new ReportDataResponse(
                "Class Attendance",
                List.of("Pupil", "Date", "Status"),
                List.of());
        }
        List<Attendance> data = attendanceRepository.findByClassIdAndDateBetween(classId, from, to);
        return attendanceReport("Class Attendance", List.of("Pupil", "Class", "Date", "Status"), data, true);
    }

    private ReportDataResponse individualAttendance(Long studentId, LocalDate from, LocalDate to) {
        List<String> columns = List.of("Date", "Class", "Status");
        List<List<Object>> rows = new ArrayList<>();
        if (studentId == null) {
            return new ReportDataResponse("Individual Attendance", columns, rows);
        }
        Map<Long, String> classNames = classNameMap();
        for (Attendance row : attendanceRepository.findByStudentIdAndDateBetween(studentId, from, to)) {
            rows.add(List.of(
                row.getDate().toString(),
                classNames.getOrDefault(row.getClassId(), "Class #" + row.getClassId()),
                row.getStatus().name()));
        }
        return new ReportDataResponse("Individual Attendance", columns, rows);
    }

    private ReportDataResponse pupilPopulation() {
        List<String> columns = List.of("Class", "Active Pupils", "Inactive Pupils", "Total");
        List<List<Object>> rows = new ArrayList<>();
        Map<Long, String> classNames = classNameMap();
        Map<Long, List<Student>> byClass = studentRepository.findAll().stream()
            .collect(Collectors.groupingBy(s -> s.getClassId() != null ? s.getClassId() : -1L));
        for (Map.Entry<Long, List<Student>> entry : byClass.entrySet()) {
            long active = entry.getValue().stream().filter(s -> Boolean.TRUE.equals(s.getIsActive())).count();
            long inactive = entry.getValue().size() - active;
            String name = entry.getKey() < 0 ? "Unassigned" : classNames.getOrDefault(entry.getKey(), "Class #" + entry.getKey());
            rows.add(List.of(name, active, inactive, entry.getValue().size()));
        }
        return new ReportDataResponse("Pupil Population", columns, rows);
    }

    private ReportDataResponse teacherStats() {
        List<String> columns = List.of("Teacher", "Active", "Assigned Classes");
        List<List<Object>> rows = new ArrayList<>();
        for (Teacher teacher : teacherRepository.findAll()) {
            long assigned = teacherClassRepository.findByTeacherIdAndIsActiveTrue(teacher.getId()).size();
            rows.add(List.of(
                teacher.getFullName() != null ? teacher.getFullName() : "Teacher #" + teacher.getId(),
                Boolean.TRUE.equals(teacher.getIsActive()),
                assigned));
        }
        return new ReportDataResponse("Teacher Stats", columns, rows);
    }

    private ReportDataResponse classStats(Long termId) {
        List<String> columns = List.of("Class", "Pupils", "Has Results", "Avg Score");
        List<List<Object>> rows = new ArrayList<>();
        Map<Long, String> classNames = classNameMap();
        for (SchoolClass schoolClass : schoolClassRepository.findAll()) {
            long pupils = studentRepository.countByClassId(schoolClass.getId());
            String avg = "";
            boolean hasResults = false;
            if (termId != null) {
                List<TermResult> results =
                    termResultRepository.findByClassIdAndTermIdOrderByAverageScoreDesc(schoolClass.getId(), termId);
                hasResults = !results.isEmpty();
                BigDecimal sum = results.stream()
                    .map(TermResult::getAverageScore)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                long count = results.stream().map(TermResult::getAverageScore).filter(Objects::nonNull).count();
                if (count > 0) {
                    avg = sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP).toPlainString();
                }
            }
            rows.add(List.of(
                classNames.getOrDefault(schoolClass.getId(), schoolClass.getName()),
                pupils,
                hasResults,
                avg));
        }
        return new ReportDataResponse("Class Stats", columns, rows);
    }

    private ReportDataResponse admissionStats() {
        List<String> columns = List.of("Status", "Count");
        List<List<Object>> rows = new ArrayList<>();
        for (AdmissionApplicationStatus status : AdmissionApplicationStatus.values()) {
            rows.add(List.of(status.name(), admissionApplicationRepository.countByStatus(status)));
        }
        List<AdmissionApplication> recent =
            admissionApplicationRepository.findTop8ByStatusOrderByCreatedAtDesc(AdmissionApplicationStatus.PENDING);
        rows.add(List.of("PENDING_SAMPLE", recent.size()));
        return new ReportDataResponse("Admission Stats", columns, rows);
    }

    private ReportDataResponse attendanceReport(String title, List<String> columns, List<Attendance> data) {
        return attendanceReport(title, columns, data, false);
    }

    private ReportDataResponse attendanceReport(
        String title, List<String> columns, List<Attendance> data, boolean includeDate) {
        Map<Long, Student> students = studentMap(data.stream().map(Attendance::getStudentId).toList());
        Map<Long, String> classNames = classNameMap();
        List<List<Object>> rows = new ArrayList<>();
        for (Attendance row : data) {
            Student student = students.get(row.getStudentId());
            if (includeDate) {
                rows.add(List.of(
                    studentName(student),
                    classNames.getOrDefault(row.getClassId(), "Class #" + row.getClassId()),
                    row.getDate().toString(),
                    row.getStatus().name()));
            } else {
                rows.add(List.of(
                    studentName(student),
                    classNames.getOrDefault(row.getClassId(), "Class #" + row.getClassId()),
                    row.getStatus().name()));
            }
        }
        return new ReportDataResponse(title, columns, rows);
    }

    private Long tryActiveTermId() {
        try {
            return activeTermService.requireActiveTermId();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Map<Long, String> classNameMap() {
        return schoolClassRepository.findAll().stream()
            .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName, (a, b) -> a));
    }

    private Map<Long, Student> studentMap(List<Long> ids) {
        return studentRepository.findAllById(ids.stream().filter(Objects::nonNull).distinct().toList()).stream()
            .collect(Collectors.toMap(Student::getId, s -> s, (a, b) -> a));
    }

    private static String studentName(Student student) {
        if (student == null) {
            return "";
        }
        String name = ((student.getFirstName() != null ? student.getFirstName() : "") + " "
            + (student.getLastName() != null ? student.getLastName() : "")).trim();
        return name.isBlank() ? "Student #" + student.getId() : name;
    }

    private static BigDecimal averageScore(List<Grade> grades) {
        List<BigDecimal> scores = grades.stream()
            .map(Grade::getScore)
            .filter(Objects::nonNull)
            .toList();
        if (scores.isEmpty()) {
            return null;
        }
        BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }
}
