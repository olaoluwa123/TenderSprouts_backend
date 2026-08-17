package com.example.backend.dto.fee;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateFeeStructureRequest(
    String name,
    @DecimalMin("0.01") BigDecimal amount,
    String description,
    LocalDate dueDate,
    Boolean isActive
) {
}
