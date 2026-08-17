package com.example.backend.dto.fee;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SaveFeeTemplateRequest(
    @NotNull @Valid List<FeeTemplateItemRequest> items
) {
}
