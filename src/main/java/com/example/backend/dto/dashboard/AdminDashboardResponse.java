package com.example.backend.dto.dashboard;

import java.util.List;

public record AdminDashboardResponse(
    Long sessionId,
    String sessionName,
    Long termId,
    String termName,
    KpiSummary kpis,
    SubmissionFunnel submissionFunnel,
    List<ClassPerformanceItem> classPerformance,
    List<GradeDistributionBucket> gradeDistribution,
    List<PendingApprovalItem> pendingApprovals,
    List<ActivityItem> recentActivity
) {
}
