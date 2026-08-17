package com.example.backend.service;

import com.example.backend.dto.admission.AdmissionEnquiryResponse;
import com.example.backend.dto.admission.CreateAdmissionEnquiryRequest;
import com.example.backend.entity.AdmissionApplication;
import com.example.backend.entity.enums.AdmissionApplicationStatus;
import com.example.backend.repository.AdmissionApplicationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdmissionEnquiryService {

    private final AdmissionApplicationRepository repository;

    public AdmissionEnquiryService(AdmissionApplicationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AdmissionEnquiryResponse create(CreateAdmissionEnquiryRequest request) {
        AdmissionApplication enquiry = new AdmissionApplication();
        enquiry.setApplicantFirstName(request.firstName().trim());
        enquiry.setApplicantLastName(request.surname().trim());
        enquiry.setParentFullName((request.firstName() + " " + request.surname()).trim());
        enquiry.setParentEmail(request.email().trim());
        enquiry.setParentPhone(trimToNull(request.phone()));
        enquiry.setChildAgeOrClass(trimToNull(request.childAgeOrClass()));
        enquiry.setNotes(request.message().trim());
        enquiry.setStatus(AdmissionApplicationStatus.PENDING);
        return toResponse(repository.save(enquiry));
    }

    @Transactional(readOnly = true)
    public List<AdmissionEnquiryResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::toResponse)
            .toList();
    }

    private AdmissionEnquiryResponse toResponse(AdmissionApplication enquiry) {
        return new AdmissionEnquiryResponse(
            enquiry.getId(),
            enquiry.getParentFullName(),
            enquiry.getParentEmail(),
            enquiry.getParentPhone(),
            enquiry.getChildAgeOrClass(),
            enquiry.getNotes(),
            enquiry.getStatus().name(),
            enquiry.getCreatedAt());
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
