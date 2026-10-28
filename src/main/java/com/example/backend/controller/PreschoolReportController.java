package com.example.backend.controller;

import com.example.backend.dto.preschool.PreschoolReportResponse;
import com.example.backend.dto.preschool.PublishPreschoolReportRequest;
import com.example.backend.dto.preschool.SavePreschoolReportRequest;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.PreschoolReportService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preschool-reports")
public class PreschoolReportController {

    private final PreschoolReportService preschoolReportService;

    public PreschoolReportController(PreschoolReportService preschoolReportService) {
        this.preschoolReportService = preschoolReportService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public PreschoolReportResponse get(
        @RequestParam Long classId,
        @RequestParam Long termId,
        @RequestParam(required = false) Integer monthNumber) {
        return preschoolReportService.getSheet(classId, termId, monthNumber, SecurityUtils.requireCurrentUser());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public PreschoolReportResponse save(@Valid @RequestBody SavePreschoolReportRequest request) {
        return preschoolReportService.saveSheet(request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/publish")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public PreschoolReportResponse publish(@Valid @RequestBody PublishPreschoolReportRequest request) {
        return preschoolReportService.publishSheet(request, SecurityUtils.requireCurrentUser());
    }
}
