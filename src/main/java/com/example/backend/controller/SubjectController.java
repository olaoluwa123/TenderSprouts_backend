package com.example.backend.controller;

import com.example.backend.dto.classroom.CreateSubjectRequest;
import com.example.backend.dto.classroom.SubjectProfileResponse;
import com.example.backend.dto.classroom.SubjectResponse;
import com.example.backend.dto.classroom.UpdateSubjectRequest;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.SubjectService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping
    public Page<SubjectResponse> list(Pageable pageable) {
        return subjectService.list(pageable);
    }

    @GetMapping("/{id}")
    public SubjectProfileResponse getProfile(@PathVariable Long id) {
        return subjectService.getProfile(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectResponse create(@Valid @RequestBody CreateSubjectRequest request) {
        return subjectService.create(request, SecurityUtils.requireCurrentUser());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SubjectResponse update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateSubjectRequest request) {
        return subjectService.update(id, request, SecurityUtils.requireCurrentUser());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete(@PathVariable Long id) {
        subjectService.softDelete(id, SecurityUtils.requireCurrentUser());
    }
}
