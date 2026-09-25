package com.example.backend.dto.assessment;

import com.example.backend.entity.enums.AssessmentSheetType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record SaveAssessmentSheetRequest(
    @NotNull AssessmentSheetType type,
    @NotNull Long classId,
    @NotNull Long termId,
    Integer weekNumber,
    @Valid List<TypeInput> types,
    @NotNull @Valid List<ColumnInput> columns,
    @NotNull @Valid List<RowInput> rows
) {
    public record TypeInput(
        @NotBlank String code,
        String name,
        @NotNull BigDecimal maxScore,
        Integer sortOrder
    ) {
    }

    public record ColumnInput(
        Long id,
        @NotBlank String clientKey,
        @NotBlank @Size(max = 100) String name,
        @NotNull Integer sortOrder,
        BigDecimal maxScore
    ) {
    }

    public record RowInput(
        @NotNull Long studentId,
        @NotNull @Valid List<CellInput> scores
    ) {
    }

    public record CellInput(
        @NotBlank String clientKey,
        @NotBlank String typeCode,
        BigDecimal score,
        @Size(max = 500) String remarks
    ) {
    }
}
