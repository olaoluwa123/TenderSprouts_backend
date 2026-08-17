package com.example.backend.service;

import com.example.backend.dto.classroom.AssignClassSubjectRequest;
import com.example.backend.dto.classroom.BulkAssignClassSubjectsRequest;
import com.example.backend.dto.classroom.BulkAssignClassSubjectsResponse;
import com.example.backend.dto.classroom.ClassListItemResponse;
import com.example.backend.dto.classroom.ClassProfileResponse;
import com.example.backend.dto.classroom.ClassProfileResponse.AttendanceInfo;
import com.example.backend.dto.classroom.ClassProfileResponse.AttendancePupil;
import com.example.backend.dto.classroom.ClassProfileResponse.PupilInfo;
import com.example.backend.dto.classroom.ClassProfileResponse.ResultItem;
import com.example.backend.dto.classroom.ClassProfileResponse.ResultsInfo;
import com.example.backend.dto.classroom.ClassProfileResponse.SubjectInfo;
import com.example.backend.dto.classroom.ClassProfileResponse.TeacherInfo;
import com.example.backend.dto.classroom.ClassSubjectResponse;
import com.example.backend.dto.classroom.CreateSchoolClassRequest;
import com.example.backend.dto.classroom.SchoolClassResponse;
import com.example.backend.entity.Attendance;
import com.example.backend.entity.ClassSubject;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Student;
import com.example.backend.entity.Subject;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.TeacherClass;
import com.example.backend.entity.Term;
import com.example.backend.entity.TermResult;
import com.example.backend.entity.enums.AttendanceStatus;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AttendanceRepository;
import com.example.backend.repository.ClassSubjectRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TeacherSubjectRepository;
import com.example.backend.repository.TermResultRepository;
import com.example.backend.security.SecurityUser;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final TermResultRepository termResultRepository;
    private final ActiveTermService activeTermService;
    private final AuditService auditService;

    public SchoolClassService(
        SchoolClassRepository schoolClassRepository,
        ClassSubjectRepository classSubjectRepository,
        SubjectRepository subjectRepository,
        TeacherClassRepository teacherClassRepository,
        TeacherRepository teacherRepository,
        TeacherSubjectRepository teacherSubjectRepository,
        StudentRepository studentRepository,
        AttendanceRepository attendanceRepository,
        TermResultRepository termResultRepository,
        ActiveTermService activeTermService,
        AuditService auditService) {
        this.schoolClassRepository = schoolClassRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.subjectRepository = subjectRepository;
        this.teacherClassRepository = teacherClassRepository;
        this.teacherRepository = teacherRepository;
        this.teacherSubjectRepository = teacherSubjectRepository;
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.termResultRepository = termResultRepository;
        this.activeTermService = activeTermService;
        this.auditService = auditService;
    }

    public Page<ClassListItemResponse> list(Pageable pageable) {
        Map<Long, String> subjectNames = subjectNameMap();
        return schoolClassRepository.findAll(pageable).map(schoolClass -> toListItem(schoolClass, subjectNames));
    }

    public ClassProfileResponse getProfile(Long classId, LocalDate attendanceDate) {
        SchoolClass schoolClass = findClass(classId);
        LocalDate date = attendanceDate != null ? attendanceDate : LocalDate.now();
        Map<Long, String> subjectNames = subjectNameMap();
        Map<Long, String> teacherNames = teacherRepository.findAll().stream()
            .collect(Collectors.toMap(Teacher::getId, Teacher::getFullName, (a, b) -> a));

        TeacherInfo teacher = resolveTeacher(classId);
        List<Student> students = studentRepository.findByClassId(classId);

        List<SubjectInfo> subjects = classSubjectRepository.findByClassIdAndIsActiveTrue(classId).stream()
            .map(cs -> {
                List<String> teachers = teacherSubjectRepository.findByClassIdAndIsActiveTrue(classId).stream()
                    .filter(ts -> ts.getSubjectId().equals(cs.getSubjectId()))
                    .map(ts -> teacherNames.getOrDefault(ts.getTeacherId(), "Teacher #" + ts.getTeacherId()))
                    .toList();
                return new SubjectInfo(
                    cs.getSubjectId(),
                    subjectNames.getOrDefault(cs.getSubjectId(), "Subject #" + cs.getSubjectId()),
                    cs.getIsActive(),
                    teachers);
            })
            .toList();

        List<PupilInfo> pupils = students.stream()
            .map(s -> new PupilInfo(
                s.getId(),
                s.getFirstName(),
                s.getLastName(),
                s.getAdmissionNumber(),
                s.getIsActive()))
            .toList();

        AttendanceInfo attendance = buildAttendance(classId, date, students);
        ResultsInfo results = buildResults(classId, students);

        return new ClassProfileResponse(
            schoolClass.getId(),
            schoolClass.getName(),
            schoolClass.getIsActive(),
            teacher,
            students.size(),
            subjects,
            pupils,
            attendance,
            results);
    }

    @Transactional
    public SchoolClassResponse create(CreateSchoolClassRequest request, SecurityUser actor) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName(request.name());
        SchoolClass saved = schoolClassRepository.save(schoolClass);
        auditService.logAdminAction(actor, "CREATE", "class", saved.getId(), saved.getName());
        return SchoolClassResponse.from(saved);
    }

    public List<ClassSubjectResponse> listSubjects(Long classId) {
        requireClass(classId);
        Map<Long, String> subjectNames = subjectNameMap();
        return classSubjectRepository.findByClassIdAndIsActiveTrue(classId).stream()
            .map(cs -> ClassSubjectResponse.from(
                cs, subjectNames.getOrDefault(cs.getSubjectId(), "Subject #" + cs.getSubjectId())))
            .toList();
    }

    @Transactional
    public ClassSubjectResponse assignSubject(Long classId, AssignClassSubjectRequest request, SecurityUser actor) {
        requireClass(classId);
        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        if (Boolean.FALSE.equals(subject.getIsActive())) {
            throw new ConflictException("Subject is inactive");
        }

        ClassSubject existing = classSubjectRepository
            .findByClassIdAndSubjectId(classId, request.subjectId())
            .orElse(null);
        ClassSubject saved;
        if (existing != null) {
            if (!Boolean.FALSE.equals(existing.getIsActive())) {
                throw new ConflictException("Subject already assigned to class");
            }
            existing.setIsActive(true);
            saved = classSubjectRepository.save(existing);
        } else {
            ClassSubject cs = new ClassSubject();
            cs.setClassId(classId);
            cs.setSubjectId(request.subjectId());
            cs.setIsActive(true);
            saved = classSubjectRepository.save(cs);
        }
        auditService.logAdminAction(actor, "ASSIGN", "class_subject", saved.getId(), null);
        return ClassSubjectResponse.from(saved, subject.getName());
    }

    @Transactional
    public void unassignSubject(Long classId, Long subjectId, SecurityUser actor) {
        requireClass(classId);
        ClassSubject existing = classSubjectRepository.findByClassIdAndSubjectId(classId, subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Class subject assignment not found"));
        existing.setIsActive(false);
        classSubjectRepository.save(existing);
        auditService.logAdminAction(actor, "UNASSIGN", "class_subject", existing.getId(),
            "classId=" + classId + ",subjectId=" + subjectId);
    }

    @Transactional
    public BulkAssignClassSubjectsResponse bulkAssignSubjects(
        BulkAssignClassSubjectsRequest request,
        SecurityUser actor) {
        Set<Long> classIds = new HashSet<>(request.classIds());
        Set<Long> subjectIds = new HashSet<>(request.subjectIds());
        for (Long classId : classIds) {
            requireClass(classId);
        }
        for (Long subjectId : subjectIds) {
            if (!subjectRepository.existsById(subjectId)) {
                throw new ResourceNotFoundException("Subject not found: " + subjectId);
            }
        }

        int assigned = 0;
        int skipped = 0;
        for (Long classId : classIds) {
            for (Long subjectId : subjectIds) {
                ClassSubject existing = classSubjectRepository.findByClassIdAndSubjectId(classId, subjectId)
                    .orElse(null);
                if (existing != null && !Boolean.FALSE.equals(existing.getIsActive())) {
                    skipped++;
                    continue;
                }
                if (existing != null) {
                    existing.setIsActive(true);
                    classSubjectRepository.save(existing);
                } else {
                    ClassSubject cs = new ClassSubject();
                    cs.setClassId(classId);
                    cs.setSubjectId(subjectId);
                    cs.setIsActive(true);
                    classSubjectRepository.save(cs);
                }
                assigned++;
            }
        }

        auditService.logAdminAction(
            actor,
            "BULK_ASSIGN",
            "class_subject",
            null,
            "assigned=" + assigned + ",skipped=" + skipped);
        return new BulkAssignClassSubjectsResponse(assigned, skipped);
    }

    private ClassListItemResponse toListItem(SchoolClass schoolClass, Map<Long, String> subjectNames) {
        TeacherInfo teacher = resolveTeacher(schoolClass.getId());
        return new ClassListItemResponse(
            schoolClass.getId(),
            schoolClass.getName(),
            schoolClass.getIsActive(),
            teacher != null ? teacher.id() : null,
            teacher != null ? teacher.fullName() : null,
            studentRepository.countByClassId(schoolClass.getId()),
            classSubjectRepository.countByClassIdAndIsActiveTrue(schoolClass.getId()));
    }

    private TeacherInfo resolveTeacher(Long classId) {
        List<TeacherClass> assignments = teacherClassRepository.findByClassIdAndIsActiveTrue(classId);
        if (assignments.isEmpty()) {
            return null;
        }
        Teacher teacher = teacherRepository.findById(assignments.get(0).getTeacherId()).orElse(null);
        if (teacher == null) {
            return null;
        }
        return new TeacherInfo(teacher.getId(), teacher.getFullName(), teacher.getPhone());
    }

    private AttendanceInfo buildAttendance(Long classId, LocalDate date, List<Student> students) {
        List<Attendance> rows = attendanceRepository.findByClassIdAndDate(classId, date);
        Map<Long, AttendanceStatus> byStudent = new HashMap<>();
        for (Attendance row : rows) {
            byStudent.put(row.getStudentId(), row.getStatus());
        }

        long present = 0;
        long absent = 0;
        long late = 0;
        long excused = 0;
        for (AttendanceStatus status : byStudent.values()) {
            switch (status) {
                case PRESENT -> present++;
                case ABSENT -> absent++;
                case LATE -> late++;
                case EXCUSED -> excused++;
            }
        }

        List<AttendancePupil> pupils = students.stream()
            .map(s -> {
                AttendanceStatus status = byStudent.get(s.getId());
                String fullName = ((s.getFirstName() != null ? s.getFirstName() : "") + " "
                    + (s.getLastName() != null ? s.getLastName() : "")).trim();
                return new AttendancePupil(
                    s.getId(),
                    fullName.isEmpty() ? "Pupil #" + s.getId() : fullName,
                    s.getAdmissionNumber(),
                    status != null ? status.name() : null);
            })
            .toList();

        return new AttendanceInfo(
            date,
            present,
            absent,
            late,
            excused,
            byStudent.size(),
            students.size(),
            pupils);
    }

    private ResultsInfo buildResults(Long classId, List<Student> students) {
        Term term;
        try {
            term = activeTermService.requireActiveTerm();
        } catch (ResourceNotFoundException ex) {
            return new ResultsInfo(null, null, List.of());
        }

        Map<Long, Student> studentMap = students.stream()
            .collect(Collectors.toMap(Student::getId, s -> s, (a, b) -> a));
        List<TermResult> rows = termResultRepository.findByClassIdAndTermIdOrderByAverageScoreDesc(
            classId, term.getId());
        List<ResultItem> items = rows.stream()
            .map(tr -> {
                Student s = studentMap.get(tr.getStudentId());
                String name = s != null
                    ? ((s.getFirstName() != null ? s.getFirstName() : "") + " "
                        + (s.getLastName() != null ? s.getLastName() : "")).trim()
                    : "Pupil #" + tr.getStudentId();
                return new ResultItem(
                    tr.getStudentId(),
                    name.isEmpty() ? "Pupil #" + tr.getStudentId() : name,
                    s != null ? s.getAdmissionNumber() : null,
                    tr.getAverageScore(),
                    tr.getRankInClass(),
                    tr.getPublishedAt());
            })
            .toList();
        return new ResultsInfo(term.getId(), term.getName(), items);
    }

    private Map<Long, String> subjectNameMap() {
        return subjectRepository.findAll().stream()
            .collect(Collectors.toMap(Subject::getId, Subject::getName, (a, b) -> a));
    }

    private SchoolClass findClass(Long classId) {
        return schoolClassRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
    }

    private void requireClass(Long classId) {
        if (!schoolClassRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found");
        }
    }
}
