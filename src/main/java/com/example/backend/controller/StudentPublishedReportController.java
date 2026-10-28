package com.example.backend.controller;

import com.example.backend.dto.parentreport.PublishedStudentReportItem;
import com.example.backend.dto.parentreport.StudentAssessmentReportResponse;
import com.example.backend.dto.parentreport.StudentBehaviouralReportView;
import com.example.backend.dto.parentreport.StudentPreschoolReportResponse;
import com.example.backend.entity.enums.AssessmentSheetType;
import com.example.backend.entity.enums.BehaviourPeriodType;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.ParentPublishedReportService;
import java.util.List;
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
@RequestMapping("/api/students/{studentId}/published-reports")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER','PARENT')")
public class StudentPublishedReportController {

    private final ParentPublishedReportService parentPublishedReportService;

    public StudentPublishedReportController(ParentPublishedReportService parentPublishedReportService) {
        this.parentPublishedReportService = parentPublishedReportService;
    }

    @GetMapping
    public List<PublishedStudentReportItem> list(@PathVariable Long studentId) {
        return parentPublishedReportService.listPublished(studentId, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/assessment-sheets")
    public StudentAssessmentReportResponse assessment(
        @PathVariable Long studentId,
        @RequestParam AssessmentSheetType type,
        @RequestParam Long termId,
        @RequestParam(required = false) Integer weekNumber) {
        return parentPublishedReportService.getAssessmentReport(
            studentId, type, termId, weekNumber, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/assessment-sheets/pdf")
    public ResponseEntity<byte[]> assessmentPdf(
        @PathVariable Long studentId,
        @RequestParam AssessmentSheetType type,
        @RequestParam Long termId,
        @RequestParam(required = false) Integer weekNumber) {
        byte[] pdf = parentPublishedReportService.assessmentPdf(
            studentId, type, termId, weekNumber, SecurityUtils.requireCurrentUser());
        String filename = "MIDTERM".equals(type.name())
            ? "midterm-" + studentId + "-term-" + termId + ".pdf"
            : "weekly-test-" + studentId + "-week-" + weekNumber + ".pdf";
        return pdfResponse(filename, pdf);
    }

    @GetMapping("/behavioural")
    public StudentBehaviouralReportView behavioural(
        @PathVariable Long studentId,
        @RequestParam Long termId,
        @RequestParam BehaviourPeriodType periodType,
        @RequestParam(required = false) Integer weekNumber) {
        return parentPublishedReportService.getBehaviouralReport(
            studentId, termId, periodType, weekNumber, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/behavioural/pdf")
    public ResponseEntity<byte[]> behaviouralPdf(
        @PathVariable Long studentId,
        @RequestParam Long termId,
        @RequestParam BehaviourPeriodType periodType,
        @RequestParam(required = false) Integer weekNumber) {
        byte[] pdf = parentPublishedReportService.behaviouralPdf(
            studentId, termId, periodType, weekNumber, SecurityUtils.requireCurrentUser());
        String filename = "behavioural-" + studentId + "-" + periodType.name().toLowerCase()
            + (weekNumber != null && weekNumber > 0 ? "-week-" + weekNumber : "")
            + ".pdf";
        return pdfResponse(filename, pdf);
    }

    @GetMapping("/preschool")
    public StudentPreschoolReportResponse preschool(
        @PathVariable Long studentId,
        @RequestParam Long termId,
        @RequestParam(required = false) Integer monthNumber) {
        return parentPublishedReportService.getPreschoolReport(
            studentId, termId, monthNumber, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/preschool/pdf")
    public ResponseEntity<byte[]> preschoolPdf(
        @PathVariable Long studentId,
        @RequestParam Long termId,
        @RequestParam(required = false) Integer monthNumber) {
        byte[] pdf = parentPublishedReportService.preschoolPdf(
            studentId, termId, monthNumber, SecurityUtils.requireCurrentUser());
        int month = monthNumber == null ? 0 : monthNumber;
        String filename = month > 0
            ? "preschool-" + studentId + "-term-" + termId + "-month-" + month + ".pdf"
            : "preschool-" + studentId + "-term-" + termId + ".pdf";
        return pdfResponse(filename, pdf);
    }

    private static ResponseEntity<byte[]> pdfResponse(String filename, byte[] pdf) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
}
