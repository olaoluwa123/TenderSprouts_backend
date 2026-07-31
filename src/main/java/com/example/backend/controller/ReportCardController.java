package com.example.backend.controller;

import com.example.backend.dto.reportcard.ReportCardResponse;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.ReportCardService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students/{studentId}/report-cards")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER','PARENT')")
public class ReportCardController {

    private final ReportCardService reportCardService;

    public ReportCardController(ReportCardService reportCardService) {
        this.reportCardService = reportCardService;
    }

    @GetMapping("/{termId}")
    public ReportCardResponse getReportCard(
        @PathVariable Long studentId,
        @PathVariable Long termId) {
        return reportCardService.getReportCard(studentId, termId, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/{termId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
        @PathVariable Long studentId,
        @PathVariable Long termId) {
        byte[] pdf = reportCardService.generatePdf(studentId, termId, SecurityUtils.requireCurrentUser());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"report-card-" + studentId + "-term-" + termId + ".pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
}
