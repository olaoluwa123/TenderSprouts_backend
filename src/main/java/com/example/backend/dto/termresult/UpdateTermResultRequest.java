package com.example.backend.dto.termresult;

import java.math.BigDecimal;

public record UpdateTermResultRequest(
    BigDecimal averageScore,
    String comments
) {
}
