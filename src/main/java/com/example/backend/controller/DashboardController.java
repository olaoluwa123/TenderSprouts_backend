package com.example.backend.controller;

import com.example.backend.dto.dashboard.ActivityItem;
import com.example.backend.dto.dashboard.AdminDashboardResponse;
import com.example.backend.dto.dashboard.TeacherDashboardResponse;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.AdminDashboardService;
import com.example.backend.service.TeacherDashboardService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final AdminDashboardService adminDashboardService;
    private final TeacherDashboardService teacherDashboardService;

    public DashboardController(
        AdminDashboardService adminDashboardService,
        TeacherDashboardService teacherDashboardService) {
        this.adminDashboardService = adminDashboardService;
        this.teacherDashboardService = teacherDashboardService;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminDashboardResponse adminDashboard() {
        return adminDashboardService.getDashboard(SecurityUtils.requireCurrentUser());
    }

    /**
     * Kept for older clients that requested the nested path.
     */
    @GetMapping({"/activity", "/admin/activity"})
    @PreAuthorize("hasRole('ADMIN')")
    public List<ActivityItem> adminActivity(
        @RequestParam(defaultValue = "50") int limit) {
        return adminDashboardService.listRecentActivity(SecurityUtils.requireCurrentUser(), limit);
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public TeacherDashboardResponse teacherDashboard() {
        return teacherDashboardService.getDashboard(SecurityUtils.requireCurrentUser());
    }
}
