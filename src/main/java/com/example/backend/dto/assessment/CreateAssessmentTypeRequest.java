package com.example.backend.dto.assessment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateAssessmentTypeRequest(
    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Code must be uppercase letters, numbers, or underscores")
    String code,
    @NotBlank
    @Size(max = 50)
    String name,
    @NotNull
    @DecimalMin(value = "0.01", message = "Max score must be greater than zero")
    BigDecimal defaultMaxScore,
    @DecimalMin(value = "0", message = "Weight cannot be negative")
    BigDecimal weight
) {
}
