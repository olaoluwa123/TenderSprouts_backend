package com.example.backend.dto.behaviour;

import com.example.backend.entity.enums.BehaviourPeriodType;
import com.example.backend.entity.enums.BehaviourRating;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveBehaviouralReportsRequest(
    @NotNull Long classId,
    @NotNull Long termId,
    @NotNull BehaviourPeriodType periodType,
    @Min(0) @Max(16) Integer weekNumber,
    @NotEmpty @Valid List<Entry> entries
) {
    public record Entry(
        @NotNull Long studentId,
        BehaviourRating punctuality,
        BehaviourRating neatness,
        BehaviourRating cooperation,
        BehaviourRating responsibility,
        BehaviourRating conduct,
        @Size(max = 1000) String remarks
    ) {
    }
}
