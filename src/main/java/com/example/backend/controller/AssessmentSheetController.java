package com.example.backend.controller;

import com.example.backend.dto.assessment.AssessmentSheetResponse;
import com.example.backend.dto.assessment.PublishAssessmentSheetRequest;
import com.example.backend.dto.assessment.SaveAssessmentSheetRequest;
import com.example.backend.dto.parentreport.StudentAssessmentReportResponse;
import com.example.backend.entity.enums.AssessmentSheetType;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.AssessmentSheetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assessment-sheets")
public class AssessmentSheetController {

    private final AssessmentSheetService assessmentSheetService;

    public AssessmentSheetController(AssessmentSheetService assessmentSheetService) {
        this.assessmentSheetService = assessmentSheetService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public AssessmentSheetResponse get(
        @RequestParam AssessmentSheetType type,
        @RequestParam Long classId,
        @RequestParam Long termId,
        @RequestParam(required = false) Integer weekNumber) {
        return assessmentSheetService.getSheet(
            type, classId, termId, weekNumber, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public StudentAssessmentReportResponse preview(
        @RequestParam AssessmentSheetType type,
        @RequestParam Long classId,
        @RequestParam Long termId,
        @RequestParam(required = false) Integer weekNumber,
        @RequestParam Long studentId) {
        return assessmentSheetService.preview(
            type, classId, termId, weekNumber, studentId, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/preview/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<byte[]> previewPdf(
        @RequestParam AssessmentSheetType type,
        @RequestParam Long classId,
        @RequestParam Long termId,
        @RequestParam(required = false) Integer weekNumber,
        @RequestParam Long studentId) {
        byte[] pdf = assessmentSheetService.previewPdf(
            type, classId, termId, weekNumber, studentId, SecurityUtils.requireCurrentUser());
        String filename = type == AssessmentSheetType.MIDTERM
            ? "midterm-" + studentId + "-term-" + termId + ".pdf"
            : "weekly-test-" + studentId + "-week-" + weekNumber + ".pdf";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public AssessmentSheetResponse save(@Valid @RequestBody SaveAssessmentSheetRequest request) {
        return assessmentSheetService.saveSheet(request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/publish")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public AssessmentSheetResponse publish(@Valid @RequestBody PublishAssessmentSheetRequest request) {
        return assessmentSheetService.publishSheet(request, SecurityUtils.requireCurrentUser());
    }
}
