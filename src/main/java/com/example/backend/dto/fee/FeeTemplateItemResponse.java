package com.example.backend.dto.fee;

import java.math.BigDecimal;

public record FeeTemplateItemResponse(
    Long id,
    String name,
    BigDecimal defaultAmount,
    String description,
    Integer sortOrder,
    Boolean isActive
) {
}
