package com.example.backend.controller;

import com.example.backend.dto.report.ReportDataResponse;
import com.example.backend.service.ReportService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/{key}")
    public ReportDataResponse get(
        @PathVariable String key,
        @RequestParam(required = false) Long classId,
        @RequestParam(required = false) Long subjectId,
        @RequestParam(required = false) Long studentId,
        @RequestParam(required = false) Long termId,
        @RequestParam(required = false) Long teacherId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.getReport(key, classId, subjectId, studentId, termId, teacherId, date, from, to);
    }

    @GetMapping("/{key}/export")
    public ResponseEntity<byte[]> export(
        @PathVariable String key,
        @RequestParam(defaultValue = "csv") String format,
        @RequestParam(required = false) Long classId,
        @RequestParam(required = false) Long subjectId,
        @RequestParam(required = false) Long studentId,
        @RequestParam(required = false) Long termId,
        @RequestParam(required = false) Long teacherId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] body = reportService.export(
            key, format, classId, subjectId, studentId, termId, teacherId, date, from, to);
        boolean pdf = "pdf".equalsIgnoreCase(format);
        String filename = key + (pdf ? ".pdf" : ".csv");
        MediaType mediaType = pdf
            ? MediaType.APPLICATION_PDF
            : MediaType.parseMediaType("text/csv");
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(mediaType)
            .body(body);
    }
}
