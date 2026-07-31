package com.example.backend.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record TeacherDashboardResponse(
    Long sessionId,
    String sessionName,
    Long termId,
    String termName,
    Long classId,
    String className,
    long studentCount,
    BigDecimal classAverage,
    String submissionStatus,
    GradingProgress gradingProgress,
    List<GradeDistributionBucket> gradeDistribution,
    List<StudentPerformanceItem> topPerformers,
    List<StudentPerformanceItem> bottomPerformers,
    List<UpcomingExamItem> upcomingExams
) {
}
