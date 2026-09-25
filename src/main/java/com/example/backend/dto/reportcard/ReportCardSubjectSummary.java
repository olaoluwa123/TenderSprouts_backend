package com.example.backend.dto.reportcard;

import java.math.BigDecimal;
import java.util.List;

public record ReportCardSubjectSummary(
    Long subjectId,
    String subjectName,
    BigDecimal subjectTotal,
    String letterGrade,
    List<ReportCardSubjectGrade> breakdown,
    String performanceBand
) {
}
