package com.example.backend.dto.behaviour;

import com.example.backend.entity.enums.BehaviourPeriodType;
import jakarta.validation.constraints.NotNull;

public record PublishBehaviouralReportsRequest(
    @NotNull Long classId,
    @NotNull Long termId,
    @NotNull BehaviourPeriodType periodType,
    Integer weekNumber
) {
}
