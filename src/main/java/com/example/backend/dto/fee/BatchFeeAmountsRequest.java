package com.example.backend.dto.fee;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BatchFeeAmountsRequest(
    @NotEmpty @Valid List<BatchFeeAmountItem> items
) {
}
