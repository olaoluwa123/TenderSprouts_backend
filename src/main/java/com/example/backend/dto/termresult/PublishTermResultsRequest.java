package com.example.backend.dto.termresult;

import jakarta.validation.constraints.NotNull;

public record PublishTermResultsRequest(
    @NotNull Long termId,
    @NotNull Long classId
) {
}
