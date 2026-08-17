package com.example.backend.dto.fee;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateFeeStructureRequest(
    @NotBlank String name,
    @NotNull Long classId,
    @NotNull Long termId,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    String description,
    LocalDate dueDate
) {
}
