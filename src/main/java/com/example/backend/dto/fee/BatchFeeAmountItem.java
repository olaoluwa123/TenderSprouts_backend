package com.example.backend.dto.fee;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BatchFeeAmountItem(
    @NotNull Long id,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    LocalDate dueDate
) {
}
