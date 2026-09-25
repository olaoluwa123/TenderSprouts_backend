package com.example.backend.controller;

import com.example.backend.dto.termresult.ComputeTermResultsRequest;
import com.example.backend.dto.termresult.PublishTermResultsRequest;
import com.example.backend.dto.termresult.SubmitTermResultsRequest;
import com.example.backend.dto.termresult.TermResultResponse;
import com.example.backend.dto.termresult.TermResultSubmissionResponse;
import com.example.backend.dto.termresult.TermResultSubmissionSummaryResponse;
import com.example.backend.dto.termresult.UpdateTermResultRequest;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.TermResultService;
import com.example.backend.entity.enums.TermResultSubmissionStatus;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/term-results")
public class TermResultController {

    private final TermResultService termResultService;

    public TermResultController(TermResultService termResultService) {
        this.termResultService = termResultService;
    }

    @GetMapping
    public Page<TermResultResponse> list(
        @RequestParam(required = false) Long termId,
        @RequestParam(required = false) Long classId,
        @RequestParam(required = false) Long studentId,
        Pageable pageable) {
        return termResultService.list(termId, classId, studentId, SecurityUtils.requireCurrentUser(), pageable);
    }

    @GetMapping("/submission")
    public TermResultSubmissionResponse getSubmission(
        @RequestParam Long classId,
        @RequestParam Long termId) {
        return termResultService.getSubmission(classId, termId, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/submissions")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TermResultSubmissionSummaryResponse> listSubmissions(
        @RequestParam(required = false) TermResultSubmissionStatus status,
        @RequestParam(required = false) Long sessionId) {
        return termResultService.listSubmissions(status, sessionId, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/{id}")
    public TermResultResponse getById(@PathVariable Long id) {
        return termResultService.getById(id, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/compute")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public List<TermResultResponse> compute(@Valid @RequestBody ComputeTermResultsRequest request) {
        return termResultService.compute(request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/submit")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEACHER')")
    public TermResultSubmissionResponse submit(@Valid @RequestBody SubmitTermResultsRequest request) {
        return termResultService.submit(request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TermResultResponse> publish(@Valid @RequestBody PublishTermResultsRequest request) {
        return termResultService.publish(request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TermResultResponse> approve(@Valid @RequestBody PublishTermResultsRequest request) {
        return termResultService.publish(request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public TermResultSubmissionResponse reject(@Valid @RequestBody PublishTermResultsRequest request) {
        return termResultService.reject(request, SecurityUtils.requireCurrentUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TermResultResponse update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateTermResultRequest request) {
        return termResultService.update(id, request, SecurityUtils.requireCurrentUser());
    }
}
