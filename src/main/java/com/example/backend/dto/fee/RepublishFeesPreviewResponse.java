package com.example.backend.dto.fee;

import java.util.List;

public record RepublishFeesPreviewResponse(
    Long classId,
    Long termId,
    int pupilsMissingInvoices,
    int publishedFeeItems,
    List<RepublishPupilPreviewItem> pupils
) {
}
