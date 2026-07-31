package com.example.backend.dto.parent;

import java.util.List;

public record ParentDashboardResponse(
    Long sessionId,
    List<ChildDashboardSummary> children
) {
}
