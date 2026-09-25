package com.example.backend.service;

import com.example.backend.dto.classroom.ClassProfileResponse.TeacherInfo;
import com.example.backend.dto.classroom.TeacherAssignmentResponse;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.TeacherClass;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.security.SecurityUser;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherAssignmentService {

    private final TeacherRepository teacherRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final AuditService auditService;

    public TeacherAssignmentService(
        TeacherRepository teacherRepository,
        TeacherClassRepository teacherClassRepository,
        SchoolClassRepository schoolClassRepository,
        AuditService auditService) {
        this.teacherRepository = teacherRepository;
        this.teacherClassRepository = teacherClassRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.auditService = auditService;
    }

    public List<TeacherInfo> activeTeachersForClass(Long classId) {
        if (classId == null) {
            return List.of();
        }
        return teacherClassRepository.findByClassIdAndIsActiveTrue(classId).stream()
            .map(TeacherClass::getTeacherId)
            .map(teacherRepository::findById)
            .flatMap(Optional::stream)
            .sorted(Comparator.comparing(
                teacher -> teacher.getFullName() == null ? "" : teacher.getFullName(),
                String.CASE_INSENSITIVE_ORDER))
            .map(teacher -> new TeacherInfo(teacher.getId(), teacher.getFullName(), teacher.getPhone()))
            .toList();
    }

    public String joinedTeacherNames(Long classId) {
        List<String> names = activeTeachersForClass(classId).stream()
            .map(TeacherInfo::fullName)
            .filter(name -> name != null && !name.isBlank())
            .toList();
        return names.isEmpty() ? null : String.join(" / ", names);
    }

    public List<TeacherAssignmentResponse> listClasses(Long teacherId) {
        requireTeacher(teacherId);
        return activeAssignments(teacherId).stream()
            .map(tc -> toResponse(tc, teacherId))
            .toList();
    }

    @Transactional
    public TeacherAssignmentResponse assignClass(Long teacherId, Long classId, SecurityUser actor) {
        requireTeacher(teacherId);
        requireClass(classId);
        if (teacherClassRepository.existsByTeacherIdAndClassIdAndIsActiveTrue(teacherId, classId)) {
            throw new ConflictException("Teacher is already assigned to this class");
        }
        TeacherClass saved = activateOrCreateAssignment(teacherId, classId);
        auditService.logAdminAction(actor, "ASSIGN", "teacher_class", saved.getId(), null);
        return toResponse(saved, teacherId);
    }

    @Transactional
    public TeacherAssignmentResponse reassignClass(Long teacherId, Long classId, SecurityUser actor) {
        requireTeacher(teacherId);
        requireClass(classId);

        List<TeacherClass> currentActives = activeAssignments(teacherId);
        if (currentActives.size() == 1 && currentActives.get(0).getClassId().equals(classId)) {
            throw new ConflictException("Teacher is already assigned to this class");
        }

        Long fromClassId = deactivateAllActive(teacherId);
        TeacherClass saved = activateOrCreateAssignment(teacherId, classId);
        auditService.logAdminAction(
            actor,
            "REASSIGN",
            "teacher_class",
            saved.getId(),
            fromClassId != null ? "fromClassId=" + fromClassId : null);
        return toResponse(saved, teacherId);
    }

    @Transactional
    public void unassignClass(Long teacherId, Long classId, SecurityUser actor) {
        requireTeacher(teacherId);
        if (classId == null) {
            throw new ResourceNotFoundException("Class is required to unassign");
        }
        requireClass(classId);
        TeacherClass assignment = teacherClassRepository.findByTeacherIdAndClassId(teacherId, classId)
            .filter(row -> Boolean.TRUE.equals(row.getIsActive()))
            .orElseThrow(() -> new ResourceNotFoundException("Teacher is not assigned to this class"));
        assignment.setIsActive(false);
        assignment.setEndDate(LocalDate.now());
        teacherClassRepository.save(assignment);
        auditService.logAdminAction(
            actor,
            "UNASSIGN",
            "teacher_class",
            teacherId,
            "fromClassId=" + classId);
    }

    private List<TeacherClass> activeAssignments(Long teacherId) {
        return teacherClassRepository.findByTeacherIdAndIsActiveTrue(teacherId);
    }

    private Long deactivateAllActive(Long teacherId) {
        List<TeacherClass> actives = activeAssignments(teacherId);
        Long fromClassId = actives.size() == 1 ? actives.get(0).getClassId() : null;
        LocalDate today = LocalDate.now();
        for (TeacherClass assignment : actives) {
            assignment.setIsActive(false);
            assignment.setEndDate(today);
            teacherClassRepository.save(assignment);
        }
        return fromClassId;
    }

    private TeacherClass activateOrCreateAssignment(Long teacherId, Long classId) {
        TeacherClass assignment = teacherClassRepository.findByTeacherIdAndClassId(teacherId, classId)
            .orElseGet(() -> {
                TeacherClass created = new TeacherClass();
                created.setTeacherId(teacherId);
                created.setClassId(classId);
                return created;
            });
        assignment.setIsActive(true);
        assignment.setStartDate(LocalDate.now());
        assignment.setEndDate(null);
        return teacherClassRepository.save(assignment);
    }

    private TeacherAssignmentResponse toResponse(TeacherClass assignment, Long teacherId) {
        String className = schoolClassRepository.findById(assignment.getClassId())
            .map(SchoolClass::getName)
            .orElse(null);
        return new TeacherAssignmentResponse(
            assignment.getId(),
            teacherId,
            assignment.getClassId(),
            className,
            assignment.getIsActive());
    }

    private void requireTeacher(Long teacherId) {
        if (!teacherRepository.existsById(teacherId)) {
            throw new ResourceNotFoundException("Teacher not found");
        }
    }

    private void requireClass(Long classId) {
        if (!schoolClassRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found");
        }
    }
}
