package com.example.backend.controller;

import com.example.backend.dto.grade.BatchGradeRequest;
import com.example.backend.dto.grade.CreateGradeRequest;
import com.example.backend.dto.grade.GradeResponse;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.GradeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/grades")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public GradeResponse create(@Valid @RequestBody CreateGradeRequest request) {
        return gradeService.createSingle(request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public List<GradeResponse> createBatch(@Valid @RequestBody BatchGradeRequest request) {
        return gradeService.createBatch(request.grades(), SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/students/{studentId}")
    public List<GradeResponse> listForStudent(
        @org.springframework.web.bind.annotation.PathVariable Long studentId,
        @RequestParam(required = false) Long termId) {
        return gradeService.listForStudent(studentId, termId, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/classes/{classId}")
    public List<GradeResponse> listForClass(
        @org.springframework.web.bind.annotation.PathVariable Long classId,
        @RequestParam Long termId,
        @RequestParam Long subjectId) {
        return gradeService.listForClass(classId, termId, subjectId, SecurityUtils.requireCurrentUser());
    }
}
