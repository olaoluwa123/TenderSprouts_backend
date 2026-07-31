package com.example.backend.dto.grade;

import java.math.BigDecimal;
import java.util.List;

public record GradingSheetSubjectRowResponse(
    Long subjectId,
    String subjectName,
    List<GradingSheetCellResponse> grades,
    BigDecimal total,
    String letterGrade
) {
}
