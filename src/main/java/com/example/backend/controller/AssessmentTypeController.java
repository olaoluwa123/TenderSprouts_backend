package com.example.backend.controller;

import com.example.backend.dto.assessment.AssessmentTypeResponse;
import com.example.backend.dto.assessment.CreateAssessmentTypeRequest;
import com.example.backend.dto.assessment.UpdateAssessmentTypeRequest;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.AssessmentTypeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assessment-types")
public class AssessmentTypeController {

    private final AssessmentTypeService assessmentTypeService;

    public AssessmentTypeController(AssessmentTypeService assessmentTypeService) {
        this.assessmentTypeService = assessmentTypeService;
    }

    @GetMapping
    public List<AssessmentTypeResponse> list() {
        return assessmentTypeService.list(SecurityUtils.requireCurrentUser());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public AssessmentTypeResponse create(@Valid @RequestBody CreateAssessmentTypeRequest request) {
        return assessmentTypeService.create(request, SecurityUtils.requireCurrentUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AssessmentTypeResponse update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateAssessmentTypeRequest request) {
        return assessmentTypeService.update(id, request, SecurityUtils.requireCurrentUser());
    }
}
