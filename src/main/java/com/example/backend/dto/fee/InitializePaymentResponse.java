package com.example.backend.dto.fee;

import java.math.BigDecimal;
import java.util.List;

public record InitializePaymentResponse(
    String accessCode,
    String reference,
    BigDecimal amount,
    List<Long> invoiceIds
) {
}
