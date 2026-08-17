package com.example.backend.controller;

import com.example.backend.dto.parent.CreateParentRequest;
import com.example.backend.dto.parent.LinkParentStudentRequest;
import com.example.backend.dto.parent.ParentDashboardResponse;
import com.example.backend.dto.parent.ParentListResponse;
import com.example.backend.dto.parent.ParentMeResponse;
import com.example.backend.dto.parent.ParentResponse;
import com.example.backend.dto.parent.UpdateParentMeRequest;
import com.example.backend.dto.student.CreateStudentRequest;
import com.example.backend.dto.student.StudentResponse;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.ParentDashboardService;
import com.example.backend.service.ParentService;
import com.example.backend.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parents")
public class ParentController {

    private final ParentService parentService;
    private final ParentDashboardService parentDashboardService;
    private final StudentService studentService;

    public ParentController(
        ParentService parentService,
        ParentDashboardService parentDashboardService,
        StudentService studentService) {
        this.parentService = parentService;
        this.parentDashboardService = parentDashboardService;
        this.studentService = studentService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PARENT')")
    public ParentMeResponse me() {
        return parentService.getMe(SecurityUtils.requireCurrentUser());
    }

    @PatchMapping("/me")
    @PreAuthorize("hasRole('PARENT')")
    public ParentMeResponse updateMe(@Valid @RequestBody UpdateParentMeRequest request) {
        return parentService.updateMe(request, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/me/dashboard")
    @PreAuthorize("hasRole('PARENT')")
    public ParentDashboardResponse dashboard(@RequestParam(required = false) Long studentId) {
        return parentDashboardService.getDashboard(SecurityUtils.requireCurrentUser(), studentId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Page<ParentListResponse> list(
        @RequestParam(required = false) Long classId,
        Pageable pageable) {
        return parentService.list(SecurityUtils.requireCurrentUser(), classId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ParentListResponse getById(@PathVariable Long id) {
        return parentService.getById(id, SecurityUtils.requireCurrentUser());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ParentResponse create(@Valid @RequestBody CreateParentRequest request) {
        return parentService.create(request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/{id}/students")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public void linkStudent(
        @PathVariable Long id,
        @Valid @RequestBody LinkParentStudentRequest request) {
        parentService.linkStudent(id, request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/{id}/students/create")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse createAndLinkStudent(
        @PathVariable Long id,
        @Valid @RequestBody CreateStudentRequest request) {
        return studentService.createForParent(id, request, SecurityUtils.requireCurrentUser());
    }

    @DeleteMapping("/{id}/students/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkStudent(@PathVariable Long id, @PathVariable Long studentId) {
        parentService.unlinkStudent(id, studentId, SecurityUtils.requireCurrentUser());
    }
}
