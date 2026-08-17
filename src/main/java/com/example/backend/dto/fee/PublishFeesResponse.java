package com.example.backend.dto.fee;

import java.util.List;

public record PublishFeesResponse(
    Long classId,
    Long termId,
    int structuresPublished,
    int invoicesCreated
) {
}
