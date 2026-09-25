package com.example.backend.controller;

import com.example.backend.dto.classroom.TeacherAssignmentResponse;
import com.example.backend.dto.student.ImportCsvJobAcceptedResponse;
import com.example.backend.dto.teacher.CreateTeacherTimetableSlotRequest;
import com.example.backend.dto.teacher.OnboardTeacherRequest;
import com.example.backend.dto.teacher.TeacherImportCsvJobResponse;
import com.example.backend.dto.teacher.TeacherProfileResponse;
import com.example.backend.dto.teacher.TeacherResponse;
import com.example.backend.dto.teacher.TeacherSubjectResponse;
import com.example.backend.dto.teacher.TeacherTimetableSlotResponse;
import com.example.backend.dto.teacher.UpdateTeacherRequest;
import com.example.backend.dto.teacher.UpdateTeacherTimetableSlotRequest;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.TeacherAssignmentService;
import com.example.backend.service.TeacherCsvImportService;
import com.example.backend.service.TeacherService;
import com.example.backend.service.TeacherTimetableService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final TeacherAssignmentService teacherAssignmentService;
    private final TeacherService teacherService;
    private final TeacherCsvImportService teacherCsvImportService;
    private final TeacherTimetableService teacherTimetableService;

    public TeacherController(
        TeacherAssignmentService teacherAssignmentService,
        TeacherService teacherService,
        TeacherCsvImportService teacherCsvImportService,
        TeacherTimetableService teacherTimetableService) {
        this.teacherAssignmentService = teacherAssignmentService;
        this.teacherService = teacherService;
        this.teacherCsvImportService = teacherCsvImportService;
        this.teacherTimetableService = teacherTimetableService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Page<TeacherResponse> list(Pageable pageable) {
        return teacherService.list(pageable);
    }

    @GetMapping("/{teacherId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public TeacherResponse getById(@PathVariable Long teacherId) {
        return teacherService.getById(teacherId);
    }

    @GetMapping("/{teacherId}/profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public TeacherProfileResponse getProfile(@PathVariable Long teacherId) {
        return teacherService.getProfile(teacherId);
    }

    @PatchMapping("/{teacherId}")
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherResponse update(
        @PathVariable Long teacherId,
        @Valid @RequestBody UpdateTeacherRequest request) {
        return teacherService.update(teacherId, request, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/{teacherId}/subjects")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public List<TeacherSubjectResponse> listSubjects(@PathVariable Long teacherId) {
        return teacherService.listSubjects(teacherId);
    }

    @GetMapping("/{teacherId}/timetable")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public List<TeacherTimetableSlotResponse> listTimetable(@PathVariable Long teacherId) {
        return teacherTimetableService.list(teacherId, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/{teacherId}/timetable")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherTimetableSlotResponse createTimetableSlot(
        @PathVariable Long teacherId,
        @Valid @RequestBody CreateTeacherTimetableSlotRequest request) {
        return teacherTimetableService.create(teacherId, request, SecurityUtils.requireCurrentUser());
    }

    @PutMapping("/{teacherId}/timetable/{slotId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public TeacherTimetableSlotResponse updateTimetableSlot(
        @PathVariable Long teacherId,
        @PathVariable Long slotId,
        @Valid @RequestBody UpdateTeacherTimetableSlotRequest request) {
        return teacherTimetableService.update(teacherId, slotId, request, SecurityUtils.requireCurrentUser());
    }

    @DeleteMapping("/{teacherId}/timetable/{slotId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTimetableSlot(@PathVariable Long teacherId, @PathVariable Long slotId) {
        teacherTimetableService.delete(teacherId, slotId, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/{teacherId}/subjects")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherSubjectResponse assignSubject(
        @PathVariable Long teacherId,
        @RequestParam Long classId,
        @RequestParam Long subjectId) {
        return teacherService.assignSubject(
            teacherId, classId, subjectId, SecurityUtils.requireCurrentUser());
    }

    @DeleteMapping("/{teacherId}/subjects")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignSubject(
        @PathVariable Long teacherId,
        @RequestParam Long classId,
        @RequestParam Long subjectId) {
        teacherService.unassignSubject(
            teacherId, classId, subjectId, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/onboard")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherResponse onboard(@Valid @RequestBody OnboardTeacherRequest request) {
        return teacherService.onboard(request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping(value = "/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ImportCsvJobAcceptedResponse importCsv(@RequestParam("file") MultipartFile file) {
        return teacherCsvImportService.enqueueImport(file, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/import-csv/jobs/{jobId}")
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherImportCsvJobResponse getImportCsvJob(@PathVariable Long jobId) {
        return teacherCsvImportService.getJob(jobId);
    }

    @GetMapping("/{teacherId}/classes")
    public List<TeacherAssignmentResponse> listClasses(@PathVariable Long teacherId) {
        return teacherAssignmentService.listClasses(teacherId);
    }

    @PostMapping("/{teacherId}/classes")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherAssignmentResponse assignClass(
        @PathVariable Long teacherId,
        @RequestParam Long classId) {
        return teacherAssignmentService.assignClass(teacherId, classId, SecurityUtils.requireCurrentUser());
    }

    @PutMapping("/{teacherId}/classes")
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherAssignmentResponse reassignClass(
        @PathVariable Long teacherId,
        @RequestParam Long classId) {
        return teacherAssignmentService.reassignClass(teacherId, classId, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/{teacherId}/classes/reassign")
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherAssignmentResponse reassignClassPost(
        @PathVariable Long teacherId,
        @RequestParam Long classId) {
        return teacherAssignmentService.reassignClass(teacherId, classId, SecurityUtils.requireCurrentUser());
    }

    @DeleteMapping("/{teacherId}/classes")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignClass(@PathVariable Long teacherId, @RequestParam Long classId) {
        teacherAssignmentService.unassignClass(teacherId, classId, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/{teacherId}/classes/unassign")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignClassPost(@PathVariable Long teacherId, @RequestParam Long classId) {
        teacherAssignmentService.unassignClass(teacherId, classId, SecurityUtils.requireCurrentUser());
    }
}
