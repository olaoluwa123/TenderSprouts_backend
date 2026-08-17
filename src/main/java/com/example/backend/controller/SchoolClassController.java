package com.example.backend.controller;

import com.example.backend.dto.classroom.AssignClassSubjectRequest;
import com.example.backend.dto.classroom.BulkAssignClassSubjectsRequest;
import com.example.backend.dto.classroom.BulkAssignClassSubjectsResponse;
import com.example.backend.dto.classroom.ClassListItemResponse;
import com.example.backend.dto.classroom.ClassProfileResponse;
import com.example.backend.dto.classroom.ClassSubjectResponse;
import com.example.backend.dto.classroom.CreateSchoolClassRequest;
import com.example.backend.dto.classroom.SchoolClassResponse;
import com.example.backend.dto.grade.GradeResponse;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.GradeService;
import com.example.backend.service.SchoolClassService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classes")
public class SchoolClassController {

    private final SchoolClassService schoolClassService;
    private final GradeService gradeService;

    public SchoolClassController(SchoolClassService schoolClassService, GradeService gradeService) {
        this.schoolClassService = schoolClassService;
        this.gradeService = gradeService;
    }

    @GetMapping
    public Page<ClassListItemResponse> list(Pageable pageable) {
        return schoolClassService.list(pageable);
    }

    @GetMapping("/{classId}")
    public ClassProfileResponse getProfile(
        @PathVariable Long classId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return schoolClassService.getProfile(classId, date);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolClassResponse create(@Valid @RequestBody CreateSchoolClassRequest request) {
        return schoolClassService.create(request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/subjects/bulk-assign")
    @PreAuthorize("hasRole('ADMIN')")
    public BulkAssignClassSubjectsResponse bulkAssignSubjects(
        @Valid @RequestBody BulkAssignClassSubjectsRequest request) {
        return schoolClassService.bulkAssignSubjects(request, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/{classId}/subjects")
    public List<ClassSubjectResponse> listSubjects(@PathVariable Long classId) {
        return schoolClassService.listSubjects(classId);
    }

    @PostMapping("/{classId}/subjects")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassSubjectResponse assignSubject(
        @PathVariable Long classId,
        @Valid @RequestBody AssignClassSubjectRequest request) {
        return schoolClassService.assignSubject(classId, request, SecurityUtils.requireCurrentUser());
    }

    @DeleteMapping("/{classId}/subjects/{subjectId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignSubject(@PathVariable Long classId, @PathVariable Long subjectId) {
        schoolClassService.unassignSubject(classId, subjectId, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/{classId}/grades")
    public List<GradeResponse> listGrades(
        @PathVariable Long classId,
        @RequestParam Long termId,
        @RequestParam Long subjectId) {
        return gradeService.listForClass(classId, termId, subjectId, SecurityUtils.requireCurrentUser());
    }
}
