package com.example.backend.dto.admission;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAdmissionEnquiryRequest(
    @NotBlank @Size(max = 50) String firstName,
    @NotBlank @Size(max = 50) String surname,
    @NotBlank @Email @Size(max = 150) String email,
    @Size(max = 30) String phone,
    @Size(max = 100) String childAgeOrClass,
    @NotBlank @Size(max = 2000) String message
) {
}
