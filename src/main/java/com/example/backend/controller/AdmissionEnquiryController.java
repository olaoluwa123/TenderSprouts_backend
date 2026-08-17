package com.example.backend.controller;

import com.example.backend.dto.admission.AdmissionEnquiryResponse;
import com.example.backend.dto.admission.CreateAdmissionEnquiryRequest;
import com.example.backend.service.AdmissionEnquiryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admission-enquiries")
public class AdmissionEnquiryController {

    private final AdmissionEnquiryService service;

    public AdmissionEnquiryController(AdmissionEnquiryService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdmissionEnquiryResponse create(@Valid @RequestBody CreateAdmissionEnquiryRequest request) {
        return service.create(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdmissionEnquiryResponse> list() {
        return service.list();
    }
}
