package com.example.backend.service;

import com.example.backend.dto.attendance.AttendanceMarkItem;
import com.example.backend.dto.attendance.MarkClassAttendanceRequest;
import com.example.backend.dto.attendance.MarkStaffAttendanceRequest;
import com.example.backend.dto.attendance.PupilAttendanceResponse;
import com.example.backend.dto.attendance.StaffAttendanceMarkItem;
import com.example.backend.dto.attendance.StaffAttendanceResponse;
import com.example.backend.entity.Attendance;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.StaffAttendance;
import com.example.backend.entity.Student;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.Term;
import com.example.backend.entity.enums.AttendanceStatus;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AttendanceRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StaffAttendanceRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.security.SecurityUser;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceService {

    private static final List<AttendanceStatus> ABSENCE_STATUSES =
        List.of(AttendanceStatus.ABSENT, AttendanceStatus.LATE);

    private final AttendanceRepository attendanceRepository;
    private final StaffAttendanceRepository staffAttendanceRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ActiveTermService activeTermService;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;

    public AttendanceService(
        AttendanceRepository attendanceRepository,
        StaffAttendanceRepository staffAttendanceRepository,
        StudentRepository studentRepository,
        TeacherRepository teacherRepository,
        SchoolClassRepository schoolClassRepository,
        ActiveTermService activeTermService,
        AuthorizationService authorizationService,
        AuditService auditService) {
        this.attendanceRepository = attendanceRepository;
        this.staffAttendanceRepository = staffAttendanceRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.activeTermService = activeTermService;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    public List<PupilAttendanceResponse> daily(LocalDate date, SecurityUser actor) {
        LocalDate target = date != null ? date : LocalDate.now();
        return restrictTeacherRows(mapPupilRows(attendanceRepository.findByDate(target)), actor);
    }

    public List<PupilAttendanceResponse> forClass(Long classId, LocalDate date, SecurityUser actor) {
        if (!schoolClassRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found");
        }
        authorizationService.requireTeacherForClass(actor, classId);
        LocalDate target = date != null ? date : LocalDate.now();
        Map<Long, Attendance> byStudent = attendanceRepository.findByClassIdAndDate(classId, target).stream()
            .collect(Collectors.toMap(Attendance::getStudentId, Function.identity(), (a, b) -> a));
        Map<Long, String> classNames = schoolClassRepository.findAll().stream()
            .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName, (a, b) -> a));
        String className = classNames.get(classId);
        return studentRepository.findByClassId(classId).stream()
            .map(student -> {
                Attendance row = byStudent.get(student.getId());
                return new PupilAttendanceResponse(
                    row != null ? row.getId() : null,
                    student.getId(),
                    fullName(student.getFirstName(), student.getLastName()),
                    student.getAdmissionNumber(),
                    classId,
                    className,
                    target,
                    row != null ? row.getStatus() : null,
                    row != null ? row.getMarkedBy() : null);
            })
            .toList();
    }

    public List<PupilAttendanceResponse> forPupil(
        Long studentId,
        LocalDate from,
        LocalDate to,
        SecurityUser actor) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found");
        }
        authorizationService.requireParentAccessToStudent(actor, studentId);
        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = to != null ? to : LocalDate.now();
        return mapPupilRows(attendanceRepository.findByStudentIdAndDateBetween(studentId, start, end));
    }

    public List<PupilAttendanceResponse> absences(LocalDate from, LocalDate to, Long classId, SecurityUser actor) {
        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = to != null ? to : LocalDate.now();
        if (classId != null) {
            authorizationService.requireTeacherForClass(actor, classId);
        }
        List<Attendance> rows = classId != null
            ? attendanceRepository.findByClassIdAndDateBetweenAndStatusIn(classId, start, end, ABSENCE_STATUSES)
            : attendanceRepository.findByDateBetweenAndStatusIn(start, end, ABSENCE_STATUSES);
        return restrictTeacherRows(mapPupilRows(rows), actor);
    }

    @Transactional
    public List<PupilAttendanceResponse> markClass(MarkClassAttendanceRequest request, SecurityUser actor) {
        if (!schoolClassRepository.existsById(request.classId())) {
            throw new ResourceNotFoundException("Class not found");
        }
        authorizationService.requireTeacherForClass(actor, request.classId());
        Term term = activeTermService.requireActiveTerm();
        Long sessionId = term.getSessionId();
        Long termId = term.getId();
        for (AttendanceMarkItem mark : request.marks()) {
            Student student = studentRepository.findById(mark.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + mark.studentId()));
            if (student.getClassId() == null || !student.getClassId().equals(request.classId())) {
                throw new ConflictException("Student does not belong to the selected class: " + student.getId());
            }
            Attendance row = attendanceRepository.findByStudentIdAndDate(student.getId(), request.date())
                .orElseGet(Attendance::new);
            row.setStudentId(student.getId());
            row.setClassId(request.classId());
            row.setDate(request.date());
            row.setStatus(mark.status());
            row.setMarkedBy(actor.getUserId());
            row.setSessionId(sessionId);
            row.setTermId(termId);
            attendanceRepository.save(row);
        }
        auditService.logAdminAction(
            actor, "MARK", "attendance", request.classId(), "class=" + request.classId() + " date=" + request.date());
        return forClass(request.classId(), request.date(), actor);
    }

    public List<StaffAttendanceResponse> staffDaily(LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        return mapStaffRows(staffAttendanceRepository.findByDate(target));
    }

    public List<StaffAttendanceResponse> forTeacher(Long teacherId, LocalDate from, LocalDate to) {
        if (!teacherRepository.existsById(teacherId)) {
            throw new ResourceNotFoundException("Teacher not found");
        }
        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = to != null ? to : LocalDate.now();
        return mapStaffRows(staffAttendanceRepository.findByTeacherIdAndDateBetween(teacherId, start, end));
    }

    @Transactional
    public List<StaffAttendanceResponse> markStaff(MarkStaffAttendanceRequest request, SecurityUser actor) {
        for (StaffAttendanceMarkItem mark : request.marks()) {
            if (!teacherRepository.existsById(mark.teacherId())) {
                throw new ResourceNotFoundException("Teacher not found: " + mark.teacherId());
            }
            StaffAttendance row = staffAttendanceRepository.findByTeacherIdAndDate(mark.teacherId(), request.date())
                .orElseGet(StaffAttendance::new);
            row.setTeacherId(mark.teacherId());
            row.setDate(request.date());
            row.setStatus(mark.status());
            row.setMarkedBy(actor.getUserId());
            staffAttendanceRepository.save(row);
        }
        auditService.logAdminAction(actor, "MARK", "staff_attendance", null, "date=" + request.date());
        return staffDaily(request.date());
    }

    private List<PupilAttendanceResponse> restrictTeacherRows(
        List<PupilAttendanceResponse> rows,
        SecurityUser actor) {
        if (actor == null || actor.getRole() != com.example.backend.entity.User.Role.TEACHER) {
            return rows;
        }
        java.util.Set<Long> assigned = authorizationService.getAssignedClassIds(actor);
        return rows.stream()
            .filter(row -> row.classId() != null && assigned.contains(row.classId()))
            .toList();
    }

    private List<PupilAttendanceResponse> mapPupilRows(List<Attendance> rows) {
        Map<Long, Student> students = studentRepository.findAllById(
                rows.stream().map(Attendance::getStudentId).distinct().toList()).stream()
            .collect(Collectors.toMap(Student::getId, Function.identity(), (a, b) -> a));
        Map<Long, String> classNames = schoolClassRepository.findAll().stream()
            .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName, (a, b) -> a));
        return rows.stream().map(row -> {
            Student student = students.get(row.getStudentId());
            String name = student == null ? null : fullName(student.getFirstName(), student.getLastName());
            return new PupilAttendanceResponse(
                row.getId(),
                row.getStudentId(),
                name,
                student != null ? student.getAdmissionNumber() : null,
                row.getClassId(),
                classNames.get(row.getClassId()),
                row.getDate(),
                row.getStatus(),
                row.getMarkedBy());
        }).toList();
    }

    private List<StaffAttendanceResponse> mapStaffRows(List<StaffAttendance> rows) {
        Map<Long, Teacher> teachers = teacherRepository.findAllById(
                rows.stream().map(StaffAttendance::getTeacherId).distinct().toList()).stream()
            .collect(Collectors.toMap(Teacher::getId, Function.identity(), (a, b) -> a));
        return rows.stream().map(row -> {
            Teacher teacher = teachers.get(row.getTeacherId());
            return new StaffAttendanceResponse(
                row.getId(),
                row.getTeacherId(),
                teacher != null ? teacher.getFullName() : null,
                row.getDate(),
                row.getStatus(),
                row.getMarkedBy());
        }).toList();
    }

    private static String fullName(String first, String last) {
        String value = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
        return value.isBlank() ? null : value;
    }
}
