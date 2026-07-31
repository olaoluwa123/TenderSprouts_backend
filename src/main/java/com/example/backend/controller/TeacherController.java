package com.example.backend.controller;

import com.example.backend.dto.classroom.TeacherAssignmentResponse;
import com.example.backend.dto.teacher.OnboardTeacherRequest;
import com.example.backend.dto.teacher.TeacherResponse;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.TeacherAssignmentService;
import com.example.backend.service.TeacherService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final TeacherAssignmentService teacherAssignmentService;
    private final TeacherService teacherService;

    public TeacherController(
        TeacherAssignmentService teacherAssignmentService,
        TeacherService teacherService) {
        this.teacherAssignmentService = teacherAssignmentService;
        this.teacherService = teacherService;
    }

    @PostMapping("/onboard")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherResponse onboard(@Valid @RequestBody OnboardTeacherRequest request) {
        return teacherService.onboard(request, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/{teacherId}/classes")
    public List<TeacherAssignmentResponse> listClasses(@PathVariable Long teacherId) {
        return teacherAssignmentService.listClasses(teacherId);
    }

    @PostMapping("/{teacherId}/classes")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherAssignmentResponse assignClass(
        @PathVariable Long teacherId,
        @RequestParam Long classId) {
        return teacherAssignmentService.assignClass(teacherId, classId, SecurityUtils.requireCurrentUser());
    }

    @PutMapping("/{teacherId}/classes")
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherAssignmentResponse reassignClass(
        @PathVariable Long teacherId,
        @RequestParam Long classId) {
        return teacherAssignmentService.reassignClass(teacherId, classId, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/{teacherId}/classes/reassign")
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherAssignmentResponse reassignClassPost(
        @PathVariable Long teacherId,
        @RequestParam Long classId) {
        return teacherAssignmentService.reassignClass(teacherId, classId, SecurityUtils.requireCurrentUser());
    }

    @DeleteMapping("/{teacherId}/classes")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignClass(@PathVariable Long teacherId) {
        teacherAssignmentService.unassignClass(teacherId, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/{teacherId}/classes/unassign")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignClassPost(@PathVariable Long teacherId) {
        teacherAssignmentService.unassignClass(teacherId, SecurityUtils.requireCurrentUser());
    }
}
