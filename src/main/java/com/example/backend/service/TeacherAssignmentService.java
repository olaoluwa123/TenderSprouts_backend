package com.example.backend.service;

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
import java.util.List;
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
        if (!activeAssignments(teacherId).isEmpty()) {
            throw new ConflictException(
                "Teacher is already assigned to a class. Reassign them to move to another class.");
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
    public void unassignClass(Long teacherId, SecurityUser actor) {
        requireTeacher(teacherId);
        List<TeacherClass> currentActives = activeAssignments(teacherId);
        if (currentActives.isEmpty()) {
            throw new ResourceNotFoundException("Teacher is not assigned to any class");
        }
        Long fromClassId = deactivateAllActive(teacherId);
        auditService.logAdminAction(
            actor,
            "UNASSIGN",
            "teacher_class",
            teacherId,
            fromClassId != null ? "fromClassId=" + fromClassId : null);
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
