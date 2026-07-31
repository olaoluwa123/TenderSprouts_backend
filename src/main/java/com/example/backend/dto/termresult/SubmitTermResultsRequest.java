package com.example.backend.dto.termresult;

import jakarta.validation.constraints.NotNull;

public record SubmitTermResultsRequest(
    @NotNull Long termId,
    @NotNull Long classId,
    @NotNull Long sessionId
) {
}
