package com.example.backend.dto.fee;

import java.math.BigDecimal;
import java.util.List;

public record ParentFeesResponse(
    BigDecimal totalOutstanding,
    List<ParentChildFeesResponse> children
) {
}
