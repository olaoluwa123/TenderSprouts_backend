package com.example.backend.service;

import com.example.backend.entity.TeacherClass;
import com.example.backend.entity.User;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.security.SecurityUser;
import com.example.backend.security.SecurityUtils;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private final TeacherClassRepository teacherClassRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final StudentRepository studentRepository;

    public AuthorizationService(
        TeacherClassRepository teacherClassRepository,
        ParentStudentRepository parentStudentRepository,
        StudentRepository studentRepository) {
        this.teacherClassRepository = teacherClassRepository;
        this.parentStudentRepository = parentStudentRepository;
        this.studentRepository = studentRepository;
    }

    public void requireAdmin() {
        if (!SecurityUtils.hasRole(User.Role.ADMIN)) {
            throw new ForbiddenException("Admin access required");
        }
    }

    public void requireTeacherForClass(SecurityUser user, Long classId) {
        if (user.getRole() == User.Role.ADMIN) {
            return;
        }
        if (user.getRole() != User.Role.TEACHER
            || !teacherClassRepository.existsByTeacherIdAndClassIdAndIsActiveTrue(user.getProfileId(), classId)) {
            throw new ForbiddenException("Not assigned to this class");
        }
    }

    public void requireParentAccessToStudent(SecurityUser user, Long studentId) {
        if (user.getRole() == User.Role.ADMIN) {
            return;
        }
        if (user.getRole() == User.Role.TEACHER) {
            return;
        }
        if (user.getRole() != User.Role.PARENT
            || !parentStudentRepository.existsByParentIdAndStudentId(user.getProfileId(), studentId)) {
            throw new ForbiddenException("Not linked to this student");
        }
    }

    public void requireParentOrStaffForStudentRead(SecurityUser user, Long studentId) {
        if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.TEACHER) {
            return;
        }
        requireParentAccessToStudent(user, studentId);
    }

    public Set<Long> getAssignedClassIds(SecurityUser user) {
        if (user.getRole() != User.Role.TEACHER || user.getProfileId() == null) {
            return Set.of();
        }
        return teacherClassRepository.findByTeacherId(user.getProfileId()).stream()
            .filter(tc -> Boolean.TRUE.equals(tc.getIsActive()))
            .map(TeacherClass::getClassId)
            .collect(Collectors.toSet());
    }

    public void requireTeacherAccessToParent(SecurityUser user, Long parentId) {
        if (user.getRole() == User.Role.ADMIN) {
            return;
        }
        if (user.getRole() != User.Role.TEACHER) {
            throw new ForbiddenException("Teacher access required");
        }
        Set<Long> classIds = getAssignedClassIds(user);
        if (classIds.isEmpty()) {
            throw new ForbiddenException("Not assigned to any class");
        }
        boolean hasAccess = parentStudentRepository.findByParentId(parentId).stream()
            .anyMatch(link -> studentRepository.findById(link.getStudentId())
                .map(student -> student.getClassId() != null && classIds.contains(student.getClassId()))
                .orElse(false));
        if (!hasAccess) {
            throw new ForbiddenException("Parent not linked to a student in your assigned classes");
        }
    }
}
