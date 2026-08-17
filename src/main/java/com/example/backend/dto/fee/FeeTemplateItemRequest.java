package com.example.backend.dto.fee;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record FeeTemplateItemRequest(
    @NotBlank String name,
    @NotNull @DecimalMin("0.00") BigDecimal defaultAmount,
    String description,
    Integer sortOrder,
    Boolean isActive
) {
}
