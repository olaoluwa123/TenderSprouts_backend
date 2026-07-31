package com.example.backend.dto.student;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BulkPromoteRequest(
    @NotNull Long sourceClassId,
    @NotNull Long sourceSessionId,
    @NotNull Long targetClassId,
    List<Long> studentIds
) {
}
