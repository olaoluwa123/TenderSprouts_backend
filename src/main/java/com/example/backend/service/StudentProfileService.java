package com.example.backend.service;

import com.example.backend.dto.student.StudentProfileResponse;
import com.example.backend.dto.student.StudentProfileResponse.AttendanceOverview;
import com.example.backend.dto.student.StudentProfileResponse.ClassHistoryItem;
import com.example.backend.dto.student.StudentProfileResponse.GuardianItem;
import com.example.backend.dto.student.StudentProfileResponse.ResultItem;
import com.example.backend.dto.student.StudentProfileResponse.SubjectItem;
import com.example.backend.entity.AcademicSession;
import com.example.backend.entity.ClassSubject;
import com.example.backend.entity.Parent;
import com.example.backend.entity.ParentStudent;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Student;
import com.example.backend.entity.StudentEnrollment;
import com.example.backend.entity.Term;
import com.example.backend.entity.TermResult;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.AttendanceStatus;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AcademicSessionRepository;
import com.example.backend.repository.AttendanceRepository;
import com.example.backend.repository.ClassSubjectRepository;
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentEnrollmentRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.repository.TermResultRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.util.LetterGradeUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class StudentProfileService {

    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final ParentRepository parentRepository;
    private final UserRepository userRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final TermResultRepository termResultRepository;
    private final TermRepository termRepository;
    private final AttendanceRepository attendanceRepository;
    private final ActiveSessionService activeSessionService;

    public StudentProfileService(
        StudentRepository studentRepository,
        SchoolClassRepository schoolClassRepository,
        AcademicSessionRepository academicSessionRepository,
        StudentEnrollmentRepository studentEnrollmentRepository,
        ParentStudentRepository parentStudentRepository,
        ParentRepository parentRepository,
        UserRepository userRepository,
        ClassSubjectRepository classSubjectRepository,
        SubjectRepository subjectRepository,
        TermResultRepository termResultRepository,
        TermRepository termRepository,
        AttendanceRepository attendanceRepository,
        ActiveSessionService activeSessionService) {
        this.studentRepository = studentRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.academicSessionRepository = academicSessionRepository;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.parentStudentRepository = parentStudentRepository;
        this.parentRepository = parentRepository;
        this.userRepository = userRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.subjectRepository = subjectRepository;
        this.termResultRepository = termResultRepository;
        this.termRepository = termRepository;
        this.attendanceRepository = attendanceRepository;
        this.activeSessionService = activeSessionService;
    }

    public StudentProfileResponse getProfile(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        String className = student.getClassId() != null
            ? schoolClassRepository.findById(student.getClassId()).map(SchoolClass::getName).orElse(null)
            : null;

        AcademicSession activeSession = null;
        try {
            activeSession = activeSessionService.requireActiveSession();
        } catch (Exception ignored) {
            activeSession = academicSessionRepository.findFirstByIsActiveTrue().orElse(null);
        }

        Long sessionId = activeSession != null ? activeSession.getId() : null;
        String sessionName = activeSession != null ? activeSession.getName() : null;

        if (student.getCurrentEnrollmentId() != null) {
            StudentEnrollment current = studentEnrollmentRepository.findById(student.getCurrentEnrollmentId())
                .orElse(null);
            if (current != null) {
                sessionId = current.getSessionId();
                sessionName = academicSessionRepository.findById(current.getSessionId())
                    .map(AcademicSession::getName)
                    .orElse(sessionName);
            }
        }

        Map<Long, String> classNames = schoolClassRepository.findAll().stream()
            .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName));
        Map<Long, String> sessionNames = academicSessionRepository.findAll().stream()
            .collect(Collectors.toMap(AcademicSession::getId, AcademicSession::getName));
        Map<Long, String> termNames = termRepository.findAll().stream()
            .collect(Collectors.toMap(Term::getId, Term::getName));

        return new StudentProfileResponse(
            student.getId(),
            student.getFirstName(),
            student.getLastName(),
            student.getDateOfBirth(),
            student.getGender() != null ? student.getGender().name() : null,
            student.getAdmissionNumber(),
            student.getClassId(),
            className,
            sessionId,
            sessionName,
            buildGuardians(studentId),
            student.getAddress(),
            student.getMedicalInformation(),
            student.getIsActive(),
            buildResults(studentId, classNames, sessionNames, termNames),
            buildAttendance(studentId),
            buildSubjects(student.getClassId()),
            buildClassHistory(studentId, classNames, sessionNames));
    }

    private List<GuardianItem> buildGuardians(Long studentId) {
        List<GuardianItem> guardians = new ArrayList<>();
        for (ParentStudent link : parentStudentRepository.findByStudentId(studentId)) {
            Parent parent = parentRepository.findById(link.getParentId()).orElse(null);
            if (parent == null) {
                continue;
            }
            String email = userRepository.findById(parent.getUserId()).map(User::getEmail).orElse(null);
            guardians.add(new GuardianItem(parent.getId(), parent.getFullName(), email, parent.getPhone()));
        }
        return guardians;
    }

    private List<ResultItem> buildResults(
        Long studentId,
        Map<Long, String> classNames,
        Map<Long, String> sessionNames,
        Map<Long, String> termNames) {
        return termResultRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
            .limit(12)
            .map(result -> toResultItem(result, classNames, sessionNames, termNames))
            .toList();
    }

    private ResultItem toResultItem(
        TermResult result,
        Map<Long, String> classNames,
        Map<Long, String> sessionNames,
        Map<Long, String> termNames) {
        return new ResultItem(
            result.getId(),
            result.getTermId(),
            termNames.getOrDefault(result.getTermId(), "Term #" + result.getTermId()),
            sessionNames.getOrDefault(result.getSessionId(), "Session #" + result.getSessionId()),
            classNames.getOrDefault(result.getClassId(), "Class #" + result.getClassId()),
            result.getAverageScore(),
            LetterGradeUtil.toLetter(result.getAverageScore()),
            result.getRankInClass(),
            result.getPublishedAt() != null);
    }

    private AttendanceOverview buildAttendance(Long studentId) {
        long present = attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.PRESENT);
        long absent = attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.ABSENT);
        long late = attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.LATE);
        long excused = attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.EXCUSED);
        long total = attendanceRepository.countByStudentId(studentId);
        return new AttendanceOverview(present, absent, late, excused, total);
    }

    private List<SubjectItem> buildSubjects(Long classId) {
        if (classId == null) {
            return List.of();
        }
        return classSubjectRepository.findByClassId(classId).stream()
            .filter(cs -> !Boolean.FALSE.equals(cs.getIsActive()))
            .map(ClassSubject::getSubjectId)
            .map(subjectRepository::findById)
            .flatMap(java.util.Optional::stream)
            .map(subject -> new SubjectItem(subject.getId(), subject.getName()))
            .toList();
    }

    private List<ClassHistoryItem> buildClassHistory(
        Long studentId,
        Map<Long, String> classNames,
        Map<Long, String> sessionNames) {
        return studentEnrollmentRepository.findByStudentIdOrderByStartDateDesc(studentId).stream()
            .map(enrollment -> toHistoryItem(enrollment, classNames, sessionNames))
            .toList();
    }

    private ClassHistoryItem toHistoryItem(
        StudentEnrollment enrollment,
        Map<Long, String> classNames,
        Map<Long, String> sessionNames) {
        return new ClassHistoryItem(
            enrollment.getId(),
            enrollment.getClassId(),
            classNames.getOrDefault(enrollment.getClassId(), "Class #" + enrollment.getClassId()),
            enrollment.getSessionId(),
            sessionNames.getOrDefault(enrollment.getSessionId(), "Session #" + enrollment.getSessionId()),
            enrollment.getStartDate(),
            enrollment.getEndDate(),
            enrollment.getStatus() != null ? enrollment.getStatus().name() : null);
    }
}
