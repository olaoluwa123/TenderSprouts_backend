package com.example.backend.dto.assessment;

import com.example.backend.entity.enums.AssessmentSheetType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AssessmentSheetResponse(
    Long sheetId,
    AssessmentSheetType type,
    Long classId,
    Long termId,
    Integer weekNumber,
    LocalDateTime publishedAt,
    List<ScoreType> types,
    List<Column> columns,
    List<PupilRow> pupils
) {
    public record ScoreType(
        String code,
        String name,
        BigDecimal maxScore,
        int sortOrder
    ) {
    }

    public record Column(
        Long id,
        String name,
        int sortOrder,
        BigDecimal maxScore
    ) {
    }

    public record PupilRow(
        Long studentId,
        String studentName,
        String admissionNumber,
        Map<String, Map<String, CellValue>> scores
    ) {
    }

    public record CellValue(
        BigDecimal score,
        String remarks
    ) {
    }
}
