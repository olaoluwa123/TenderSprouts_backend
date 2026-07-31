package com.example.backend.dto.assessment;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateAssessmentTypeRequest(
    @Size(max = 50)
    String name,
    @DecimalMin(value = "0.01", message = "Max score must be greater than zero")
    BigDecimal defaultMaxScore,
    @DecimalMin(value = "0", message = "Weight cannot be negative")
    BigDecimal weight,
    @JsonProperty("active")
    @JsonAlias("isActive")
    Boolean active
) {
}
