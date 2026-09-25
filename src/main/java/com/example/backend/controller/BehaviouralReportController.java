package com.example.backend.controller;

import com.example.backend.dto.behaviour.BehaviouralReportResponse;
import com.example.backend.dto.behaviour.PublishBehaviouralReportsRequest;
import com.example.backend.dto.behaviour.SaveBehaviouralReportsRequest;
import com.example.backend.entity.enums.BehaviourPeriodType;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.BehaviouralReportService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/behavioural-reports")
public class BehaviouralReportController {

    private final BehaviouralReportService behaviouralReportService;

    public BehaviouralReportController(BehaviouralReportService behaviouralReportService) {
        this.behaviouralReportService = behaviouralReportService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<BehaviouralReportResponse> list(
        @RequestParam Long classId,
        @RequestParam Long termId,
        @RequestParam BehaviourPeriodType periodType,
        @RequestParam(required = false) Integer weekNumber) {
        return behaviouralReportService.listForClass(
            classId, termId, periodType, weekNumber, SecurityUtils.requireCurrentUser());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<BehaviouralReportResponse> save(@Valid @RequestBody SaveBehaviouralReportsRequest request) {
        return behaviouralReportService.saveClass(request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/publish")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<BehaviouralReportResponse> publish(@Valid @RequestBody PublishBehaviouralReportsRequest request) {
        return behaviouralReportService.publishClass(request, SecurityUtils.requireCurrentUser());
    }
}
