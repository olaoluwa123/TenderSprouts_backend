package com.example.backend.service;

import com.example.backend.dto.behaviour.BehaviouralReportResponse;
import com.example.backend.dto.behaviour.PublishBehaviouralReportsRequest;
import com.example.backend.dto.behaviour.SaveBehaviouralReportsRequest;
import com.example.backend.entity.BehaviouralReport;
import com.example.backend.entity.Student;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.BehaviourPeriodType;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.BehaviouralReportRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.security.SecurityUser;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BehaviouralReportService {

    private final BehaviouralReportRepository behaviouralReportRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final TermRepository termRepository;
    private final TeacherRepository teacherRepository;
    private final AuthorizationService authorizationService;
    private final ActiveSessionService activeSessionService;
    private final ActiveTermService activeTermService;
    private final AuditService auditService;

    public BehaviouralReportService(
        BehaviouralReportRepository behaviouralReportRepository,
        StudentRepository studentRepository,
        SchoolClassRepository schoolClassRepository,
        TermRepository termRepository,
        TeacherRepository teacherRepository,
        AuthorizationService authorizationService,
        ActiveSessionService activeSessionService,
        ActiveTermService activeTermService,
        AuditService auditService) {
        this.behaviouralReportRepository = behaviouralReportRepository;
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
    public List<BehaviouralReportResponse> listForClass(
        Long classId,
        Long termId,
        BehaviourPeriodType periodType,
        Integer weekNumber,
        SecurityUser actor) {
        validateClassTerm(classId, termId);
        authorizationService.requireTeacherForClass(actor, classId);
        int week = normalizeWeek(periodType, weekNumber);

        Map<Long, BehaviouralReport> byStudent = new HashMap<>();
        for (BehaviouralReport report : behaviouralReportRepository
            .findByClassIdAndTermIdAndPeriodTypeAndWeekNumberOrderByStudentIdAsc(
                classId, termId, periodType, week)) {
            byStudent.put(report.getStudentId(), report);
        }

        List<BehaviouralReportResponse> rows = new ArrayList<>();
        for (Student student : activeStudentsInClass(classId)) {
            BehaviouralReport existing = byStudent.get(student.getId());
            if (existing != null) {
                rows.add(toResponse(existing, student));
            } else {
                rows.add(emptyRow(student, classId, termId, periodType, week));
            }
        }
        return rows;
    }

    @Transactional
    public List<BehaviouralReportResponse> saveClass(
        SaveBehaviouralReportsRequest request,
        SecurityUser actor) {
        validateClassTerm(request.classId(), request.termId());
        activeTermService.requireMatchesActiveTermForWrite(request.termId(), actor);
        authorizationService.requireTeacherForClass(actor, request.classId());

        BehaviourPeriodType periodType = request.periodType();
        int week = normalizeWeek(periodType, request.weekNumber());
        Long teacherId = resolveTeacherId(actor);
        Long sessionId = activeSessionService.requireActiveSessionId();

        for (SaveBehaviouralReportsRequest.Entry entry : request.entries()) {
            Student student = studentRepository.findById(entry.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + entry.studentId()));
            if (!request.classId().equals(student.getClassId())) {
                throw new ConflictException("Student does not belong to the selected class");
            }

            BehaviouralReport report = behaviouralReportRepository
                .findByStudentIdAndTermIdAndPeriodTypeAndWeekNumber(
                    entry.studentId(), request.termId(), periodType, week)
                .orElseGet(BehaviouralReport::new);
            report.setStudentId(entry.studentId());
            report.setClassId(request.classId());
            report.setTeacherId(teacherId);
            report.setSessionId(sessionId);
            report.setTermId(request.termId());
            report.setPeriodType(periodType);
            report.setWeekNumber(week);
            report.setPunctuality(entry.punctuality());
            report.setNeatness(entry.neatness());
            report.setCooperation(entry.cooperation());
            report.setResponsibility(entry.responsibility());
            report.setConduct(entry.conduct());
            report.setRemarks(blankToNull(entry.remarks()));
            behaviouralReportRepository.save(report);
        }

        auditService.logAdminAction(
            actor,
            "UPSERT",
            "behavioural_reports",
            request.classId(),
            periodType + "/week=" + week);

        return listForClass(request.classId(), request.termId(), periodType, week, actor);
    }

    @Transactional
    public List<BehaviouralReportResponse> publishClass(
        PublishBehaviouralReportsRequest request,
        SecurityUser actor) {
        validateClassTerm(request.classId(), request.termId());
        activeTermService.requireMatchesActiveTermForWrite(request.termId(), actor);
        authorizationService.requireTeacherForClass(actor, request.classId());

        BehaviourPeriodType periodType = request.periodType();
        int week = normalizeWeek(periodType, request.weekNumber());
        List<BehaviouralReport> reports = behaviouralReportRepository
            .findByClassIdAndTermIdAndPeriodTypeAndWeekNumberOrderByStudentIdAsc(
                request.classId(), request.termId(), periodType, week);
        if (reports.isEmpty()) {
            throw new ConflictException("Save the behavioural report before publishing");
        }

        LocalDateTime now = LocalDateTime.now();
        for (BehaviouralReport report : reports) {
            if (report.getPublishedAt() == null) {
                report.setPublishedAt(now);
                behaviouralReportRepository.save(report);
            }
        }

        auditService.logAdminAction(
            actor,
            "PUBLISH",
            "behavioural_reports",
            request.classId(),
            periodType + "/week=" + week);

        return listForClass(request.classId(), request.termId(), periodType, week, actor);
    }

    private void validateClassTerm(Long classId, Long termId) {
        if (!schoolClassRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found");
        }
        if (!termRepository.existsById(termId)) {
            throw new ResourceNotFoundException("Term not found");
        }
    }

    private int normalizeWeek(BehaviourPeriodType periodType, Integer weekNumber) {
        if (periodType == BehaviourPeriodType.END_OF_TERM) {
            return 0;
        }
        if (weekNumber == null || weekNumber < 1 || weekNumber > 16) {
            throw new ConflictException("Week number must be between 1 and 16 for weekly reports");
        }
        return weekNumber;
    }

    private List<Student> activeStudentsInClass(Long classId) {
        return studentRepository.findByClassId(classId).stream()
            .filter(s -> !Boolean.FALSE.equals(s.getIsActive()))
            .sorted((a, b) -> formatStudentName(a).compareToIgnoreCase(formatStudentName(b)))
            .toList();
    }

    private BehaviouralReportResponse toResponse(BehaviouralReport report, Student student) {
        return BehaviouralReportResponse.from(
            report,
            formatStudentName(student),
            student.getAdmissionNumber());
    }

    private BehaviouralReportResponse emptyRow(
        Student student,
        Long classId,
        Long termId,
        BehaviourPeriodType periodType,
        int week) {
        return new BehaviouralReportResponse(
            null,
            student.getId(),
            formatStudentName(student),
            student.getAdmissionNumber(),
            classId,
            termId,
            periodType,
            week,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
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
