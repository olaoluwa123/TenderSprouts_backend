package com.example.backend.dto.fee;

import java.util.List;

public record FeeTemplateResponse(
    Long id,
    Long classId,
    String className,
    List<FeeTemplateItemResponse> items
) {
}
