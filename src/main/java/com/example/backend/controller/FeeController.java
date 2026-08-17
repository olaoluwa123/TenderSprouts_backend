package com.example.backend.controller;

import com.example.backend.dto.fee.BatchFeeAmountsRequest;
import com.example.backend.dto.fee.CreateFeeStructureRequest;
import com.example.backend.dto.fee.FeeStructureResponse;
import com.example.backend.dto.fee.FeeTemplateResponse;
import com.example.backend.dto.fee.GeneratePupilFeesResponse;
import com.example.backend.dto.fee.InitializePaymentResponse;
import com.example.backend.dto.fee.InvoiceResponse;
import com.example.backend.dto.fee.ParentFeesResponse;
import com.example.backend.dto.fee.PublishFeesResponse;
import com.example.backend.dto.fee.RepublishFeesPreviewResponse;
import com.example.backend.dto.fee.RepublishFeesResponse;
import com.example.backend.dto.fee.SaveFeeTemplateRequest;
import com.example.backend.dto.fee.SchoolFeeTemplateResponse;
import com.example.backend.dto.fee.UpdateFeeStructureRequest;
import com.example.backend.dto.fee.VerifyPaymentRequest;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.FeePaymentService;
import com.example.backend.service.FeeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fees")
public class FeeController {

    private final FeeService feeService;
    private final FeePaymentService feePaymentService;

    public FeeController(FeeService feeService, FeePaymentService feePaymentService) {
        this.feeService = feeService;
        this.feePaymentService = feePaymentService;
    }

    @GetMapping("/templates/school")
    @PreAuthorize("hasRole('ADMIN')")
    public SchoolFeeTemplateResponse getSchoolTemplate() {
        return feeService.getSchoolTemplate();
    }

    @PutMapping("/templates/school")
    @PreAuthorize("hasRole('ADMIN')")
    public SchoolFeeTemplateResponse saveSchoolTemplate(@Valid @RequestBody SaveFeeTemplateRequest request) {
        return feeService.saveSchoolTemplate(request, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/templates/{classId}")
    @PreAuthorize("hasRole('ADMIN')")
    public FeeTemplateResponse getTemplate(@PathVariable Long classId) {
        return feeService.getTemplate(classId);
    }

    @PutMapping("/templates/{classId}")
    @PreAuthorize("hasRole('ADMIN')")
    public FeeTemplateResponse saveTemplate(
        @PathVariable Long classId,
        @Valid @RequestBody SaveFeeTemplateRequest request) {
        return feeService.saveTemplate(classId, request, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/structures")
    @PreAuthorize("hasRole('ADMIN')")
    public List<FeeStructureResponse> listStructures(
        @RequestParam Long classId,
        @RequestParam Long termId) {
        return feeService.listStructures(classId, termId);
    }

    @PostMapping("/structures")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public FeeStructureResponse createStructure(@Valid @RequestBody CreateFeeStructureRequest request) {
        return feeService.createStructure(request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/structures/ensure-from-template")
    @PreAuthorize("hasRole('ADMIN')")
    public List<FeeStructureResponse> ensureFromTemplate(
        @RequestParam Long classId,
        @RequestParam Long termId) {
        return feeService.ensureFromTemplate(classId, termId, SecurityUtils.requireCurrentUser());
    }

    @PutMapping("/structures/batch-amounts")
    @PreAuthorize("hasRole('ADMIN')")
    public List<FeeStructureResponse> batchAmounts(@Valid @RequestBody BatchFeeAmountsRequest request) {
        return feeService.batchUpdateAmounts(request, SecurityUtils.requireCurrentUser());
    }

    @PatchMapping("/structures/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public FeeStructureResponse updateStructure(
        @PathVariable Long id,
        @Valid @RequestBody UpdateFeeStructureRequest request) {
        return feeService.updateStructure(id, request, SecurityUtils.requireCurrentUser());
    }

    @DeleteMapping("/structures/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStructure(@PathVariable Long id) {
        feeService.deleteStructure(id, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/structures/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public PublishFeesResponse publish(
        @RequestParam Long classId,
        @RequestParam Long termId) {
        return feeService.publishClassTerm(classId, termId, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/republish-preview")
    @PreAuthorize("hasRole('ADMIN')")
    public RepublishFeesPreviewResponse republishPreview(
        @RequestParam Long classId,
        @RequestParam Long termId) {
        return feeService.previewRepublishForNewPupils(classId, termId);
    }

    @PostMapping("/structures/republish")
    @PreAuthorize("hasRole('ADMIN')")
    public RepublishFeesResponse republish(
        @RequestParam Long classId,
        @RequestParam Long termId) {
        return feeService.republishForNewPupils(classId, termId, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasRole('ADMIN')")
    public List<InvoiceResponse> listInvoices(
        @RequestParam Long classId,
        @RequestParam Long termId) {
        return feeService.listInvoicesForClassTerm(classId, termId);
    }

    @PostMapping("/invoices/{id}/mark-paid")
    @PreAuthorize("hasRole('ADMIN')")
    public InvoiceResponse markPaid(@PathVariable Long id) {
        return feeService.markInvoicePaid(id, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/payments/initialize")
    @PreAuthorize("hasRole('PARENT')")
    public InitializePaymentResponse initializePayment() {
        return feePaymentService.initializeForParent(SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/payments/verify")
    @PreAuthorize("hasRole('PARENT')")
    public ParentFeesResponse verifyPayment(@Valid @RequestBody VerifyPaymentRequest request) {
        return feePaymentService.verify(request.reference(), SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/payments/webhook")
    public ResponseEntity<String> paystackWebhook(
        @RequestHeader(value = "x-paystack-signature", required = false) String signature,
        HttpServletRequest request) throws IOException {
        String rawBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        feePaymentService.handleWebhook(signature, rawBody);
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PARENT')")
    public ParentFeesResponse myFees() {
        return feeService.parentFees(SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/students/{studentId}/generate")
    @PreAuthorize("hasRole('PARENT')")
    public GeneratePupilFeesResponse generateForStudent(@PathVariable Long studentId) {
        return feeService.generateFeesForStudent(studentId, SecurityUtils.requireCurrentUser());
    }
}
