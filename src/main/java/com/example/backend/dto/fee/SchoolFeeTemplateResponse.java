package com.example.backend.dto.fee;

import java.util.List;

public record SchoolFeeTemplateResponse(
    List<FeeTemplateItemResponse> items,
    int classesSynced
) {
}
