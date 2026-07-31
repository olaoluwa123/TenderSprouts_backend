package com.example.backend.dto.termresult;

import jakarta.validation.constraints.NotNull;

public record ComputeTermResultsRequest(
    @NotNull Long termId,
    @NotNull Long classId,
    @NotNull Long sessionId
) {
}
