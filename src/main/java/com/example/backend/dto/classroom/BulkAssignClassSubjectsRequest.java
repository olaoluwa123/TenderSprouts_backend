package com.example.backend.dto.classroom;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkAssignClassSubjectsRequest(
    @NotEmpty List<Long> classIds,
    @NotEmpty List<Long> subjectIds
) {
}
