package com.example.backend.dto.assessment;

import com.example.backend.entity.AssessmentTypeLookup;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record AssessmentTypeResponse(
    Long id,
    String code,
    String name,
    BigDecimal defaultMaxScore,
    BigDecimal weight,
    @JsonProperty("active")
    @JsonAlias("isActive")
    boolean active
) {
    public static AssessmentTypeResponse from(AssessmentTypeLookup type) {
        return new AssessmentTypeResponse(
            type.getId(),
            type.getCode(),
            type.getName(),
            type.getDefaultMaxScore(),
            type.getWeight(),
            Boolean.TRUE.equals(type.getIsActive()));
    }
}
