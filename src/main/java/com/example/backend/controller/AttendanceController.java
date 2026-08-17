package com.example.backend.controller;

import com.example.backend.dto.attendance.MarkClassAttendanceRequest;
import com.example.backend.dto.attendance.MarkStaffAttendanceRequest;
import com.example.backend.dto.attendance.PupilAttendanceResponse;
import com.example.backend.dto.attendance.StaffAttendanceResponse;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.AttendanceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public List<PupilAttendanceResponse> daily(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return attendanceService.daily(date);
    }

    @GetMapping("/class")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public List<PupilAttendanceResponse> forClass(
        @RequestParam Long classId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return attendanceService.forClass(classId, date);
    }

    @GetMapping("/pupil")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public List<PupilAttendanceResponse> forPupil(
        @RequestParam Long studentId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return attendanceService.forPupil(studentId, from, to, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/absences")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public List<PupilAttendanceResponse> absences(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) Long classId) {
        return attendanceService.absences(from, to, classId);
    }

    @PostMapping("/class/mark")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public List<PupilAttendanceResponse> markClass(@Valid @RequestBody MarkClassAttendanceRequest request) {
        return attendanceService.markClass(request, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StaffAttendanceResponse> staffDaily(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return attendanceService.staffDaily(date);
    }

    @GetMapping("/staff/teacher")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StaffAttendanceResponse> forTeacher(
        @RequestParam Long teacherId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return attendanceService.forTeacher(teacherId, from, to);
    }

    @PostMapping("/staff/mark")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StaffAttendanceResponse> markStaff(@Valid @RequestBody MarkStaffAttendanceRequest request) {
        return attendanceService.markStaff(request, SecurityUtils.requireCurrentUser());
    }
}
