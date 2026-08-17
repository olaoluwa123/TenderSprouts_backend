package com.example.backend.dto.fee;

import jakarta.validation.constraints.NotBlank;

public record VerifyPaymentRequest(
    @NotBlank String reference
) {
}
