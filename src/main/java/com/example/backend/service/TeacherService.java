package com.example.backend.service;

import com.example.backend.dto.teacher.OnboardTeacherRequest;
import com.example.backend.dto.teacher.TeacherProfileResponse;
import com.example.backend.dto.teacher.TeacherProfileResponse.ClassItem;
import com.example.backend.dto.teacher.TeacherProfileResponse.SubjectItem;
import com.example.backend.dto.teacher.TeacherResponse;
import com.example.backend.dto.teacher.TeacherResponse.ClassSummary;
import com.example.backend.dto.teacher.TeacherSubjectResponse;
import com.example.backend.dto.teacher.UpdateTeacherRequest;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Subject;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.TeacherClass;
import com.example.backend.entity.TeacherSubject;
import com.example.backend.entity.User;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ClassSubjectRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TeacherSubjectRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.SecurityUser;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final PortalUserOnboardingService portalUserOnboardingService;
    private final TeacherAssignmentService teacherAssignmentService;
    private final AuditService auditService;

    public TeacherService(
        TeacherRepository teacherRepository,
        UserRepository userRepository,
        TeacherClassRepository teacherClassRepository,
        TeacherSubjectRepository teacherSubjectRepository,
        SchoolClassRepository schoolClassRepository,
        SubjectRepository subjectRepository,
        ClassSubjectRepository classSubjectRepository,
        PortalUserOnboardingService portalUserOnboardingService,
        TeacherAssignmentService teacherAssignmentService,
        AuditService auditService) {
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.teacherClassRepository = teacherClassRepository;
        this.teacherSubjectRepository = teacherSubjectRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.portalUserOnboardingService = portalUserOnboardingService;
        this.teacherAssignmentService = teacherAssignmentService;
        this.auditService = auditService;
    }

    public Page<TeacherResponse> list(Pageable pageable, SecurityUser actor) {
        Map<Long, String> classNames = classNameMap();
        if (actor != null && actor.getRole() == User.Role.TEACHER) {
            if (actor.getProfileId() == null) {
                return Page.empty(pageable);
            }
            Teacher self = findTeacher(actor.getProfileId());
            return new PageImpl<>(List.of(toResponse(self, classNames)), pageable, 1);
        }
        return teacherRepository.findAll(pageable).map(teacher -> toResponse(teacher, classNames));
    }

    public TeacherResponse getById(Long id) {
        return toResponse(findTeacher(id), classNameMap());
    }

    public TeacherProfileResponse getProfile(Long id) {
        Teacher teacher = findTeacher(id);
        User user = userRepository.findById(teacher.getUserId()).orElse(null);
        Map<Long, String> classNames = classNameMap();
        Map<Long, String> subjectNames = subjectRepository.findAll().stream()
            .collect(Collectors.toMap(Subject::getId, Subject::getName));

        List<ClassItem> classes = teacherClassRepository.findByTeacherIdAndIsActiveTrue(id).stream()
            .map(tc -> new ClassItem(
                tc.getId(),
                tc.getClassId(),
                classNames.getOrDefault(tc.getClassId(), "Class #" + tc.getClassId()),
                tc.getStartDate(),
                tc.getIsActive()))
            .toList();

        List<SubjectItem> subjects = teacherSubjectRepository.findByTeacherId(id).stream()
            .filter(ts -> !Boolean.FALSE.equals(ts.getIsActive()))
            .map(ts -> new SubjectItem(
                ts.getId(),
                ts.getSubjectId(),
                subjectNames.getOrDefault(ts.getSubjectId(), "Subject #" + ts.getSubjectId()),
                ts.getClassId(),
                classNames.getOrDefault(ts.getClassId(), "Class #" + ts.getClassId()),
                ts.getIsActive()))
            .toList();

        return new TeacherProfileResponse(
            teacher.getId(),
            teacher.getUserId(),
            user != null ? user.getEmail() : null,
            teacher.getFullName(),
            teacher.getPhone(),
            teacher.getIsActive(),
            user != null ? user.getIsActive() : null,
            classes,
            subjects);
    }

    @Transactional
    public TeacherResponse update(Long id, UpdateTeacherRequest request, SecurityUser actor) {
        Teacher teacher = findTeacher(id);
        if (request.fullName() != null) {
            teacher.setFullName(request.fullName());
        }
        if (request.phone() != null) {
            teacher.setPhone(request.phone());
        }
        if (request.isActive() != null) {
            teacher.setIsActive(request.isActive());
        }
        Teacher saved = teacherRepository.save(teacher);
        auditService.logAdminAction(actor, "UPDATE", "teacher", saved.getId(), saved.getFullName());
        return toResponse(saved, classNameMap());
    }

    public List<TeacherSubjectResponse> listSubjects(Long teacherId) {
        findTeacher(teacherId);
        Map<Long, String> classNames = classNameMap();
        Map<Long, String> subjectNames = subjectRepository.findAll().stream()
            .collect(Collectors.toMap(Subject::getId, Subject::getName));
        return teacherSubjectRepository.findByTeacherId(teacherId).stream()
            .filter(ts -> !Boolean.FALSE.equals(ts.getIsActive()))
            .map(ts -> toSubjectResponse(ts, classNames, subjectNames))
            .toList();
    }

    @Transactional
    public TeacherSubjectResponse assignSubject(
        Long teacherId,
        Long classId,
        Long subjectId,
        SecurityUser actor) {
        findTeacher(teacherId);
        if (!teacherClassRepository.existsByTeacherIdAndClassIdAndIsActiveTrue(teacherId, classId)) {
            throw new ConflictException("Teacher is not assigned to that class");
        }
        if (classSubjectRepository.findByClassIdAndSubjectId(classId, subjectId).isEmpty()) {
            throw new ConflictException("Subject is not on that class curriculum");
        }
        if (teacherSubjectRepository.existsByTeacherIdAndClassIdAndSubjectIdAndIsActiveTrue(
            teacherId, classId, subjectId)) {
            throw new ConflictException("Subject already assigned to this teacher for that class");
        }

        TeacherSubject existing = teacherSubjectRepository
            .findByTeacherIdAndClassIdAndSubjectId(teacherId, classId, subjectId)
            .orElse(null);
        TeacherSubject saved;
        if (existing != null) {
            existing.setIsActive(true);
            saved = teacherSubjectRepository.save(existing);
        } else {
            TeacherSubject row = new TeacherSubject();
            row.setTeacherId(teacherId);
            row.setClassId(classId);
            row.setSubjectId(subjectId);
            row.setIsActive(true);
            saved = teacherSubjectRepository.save(row);
        }

        auditService.logAdminAction(actor, "ASSIGN", "teacher_subject", saved.getId(),
            "teacherId=" + teacherId + ",classId=" + classId + ",subjectId=" + subjectId);

        Map<Long, String> classNames = classNameMap();
        Map<Long, String> subjectNames = subjectRepository.findAll().stream()
            .collect(Collectors.toMap(Subject::getId, Subject::getName));
        return toSubjectResponse(saved, classNames, subjectNames);
    }

    @Transactional
    public void unassignSubject(Long teacherId, Long classId, Long subjectId, SecurityUser actor) {
        findTeacher(teacherId);
        TeacherSubject existing = teacherSubjectRepository
            .findByTeacherIdAndClassIdAndSubjectId(teacherId, classId, subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher subject assignment not found"));
        existing.setIsActive(false);
        teacherSubjectRepository.save(existing);
        auditService.logAdminAction(actor, "UNASSIGN", "teacher_subject", existing.getId(),
            "teacherId=" + teacherId + ",classId=" + classId + ",subjectId=" + subjectId);
    }

    @Transactional
    public TeacherResponse onboard(OnboardTeacherRequest request, SecurityUser actor) {
        PortalUserOnboardingService.OnboardedUser onboarded =
            portalUserOnboardingService.createUserWithTemporaryPassword(request.email(), User.Role.TEACHER);

        Teacher teacher = new Teacher();
        teacher.setUserId(onboarded.user().getId());
        teacher.setFullName(request.fullName());
        teacher.setPhone(request.phone());
        teacher.setIsActive(true);
        teacher = teacherRepository.save(teacher);

        if (request.classId() != null) {
            teacherAssignmentService.assignClass(teacher.getId(), request.classId(), actor);
        }

        portalUserOnboardingService.sendWelcomeCredentialsEmail(
            onboarded.user(),
            request.fullName(),
            "teacher",
            onboarded.plainPassword());

        auditService.logAdminAction(actor, "ONBOARD_TEACHER", "teacher", teacher.getId(), teacher.getFullName());
        return toResponse(teacher, classNameMap());
    }

    private TeacherResponse toResponse(Teacher teacher, Map<Long, String> classNames) {
        User user = userRepository.findById(teacher.getUserId()).orElse(null);
        List<ClassSummary> classes = teacherClassRepository.findByTeacherIdAndIsActiveTrue(teacher.getId()).stream()
            .map(tc -> new ClassSummary(
                tc.getClassId(),
                classNames.getOrDefault(tc.getClassId(), "Class #" + tc.getClassId())))
            .toList();
        Long classId = classes.isEmpty() ? null : classes.get(0).classId();
        String className = classId != null
            ? classNames.getOrDefault(classId, "Class #" + classId)
            : null;
        return TeacherResponse.from(
            teacher,
            user != null ? user.getEmail() : null,
            user != null ? user.getIsActive() : null,
            classId,
            className,
            classes);
    }

    private TeacherSubjectResponse toSubjectResponse(
        TeacherSubject ts,
        Map<Long, String> classNames,
        Map<Long, String> subjectNames) {
        return new TeacherSubjectResponse(
            ts.getId(),
            ts.getTeacherId(),
            ts.getSubjectId(),
            subjectNames.getOrDefault(ts.getSubjectId(), "Subject #" + ts.getSubjectId()),
            ts.getClassId(),
            classNames.getOrDefault(ts.getClassId(), "Class #" + ts.getClassId()),
            ts.getIsActive());
    }

    private Map<Long, String> classNameMap() {
        return schoolClassRepository.findAll().stream()
            .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName));
    }

    private Teacher findTeacher(Long id) {
        return teacherRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
    }
}
