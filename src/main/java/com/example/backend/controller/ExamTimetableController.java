package com.example.backend.controller;

import com.example.backend.dto.examtimetable.CreateExamTimetableSlotRequest;
import com.example.backend.dto.examtimetable.ExamTimetableSlotResponse;
import com.example.backend.dto.examtimetable.PublishExamTimetableResponse;
import com.example.backend.dto.examtimetable.UpdateExamTimetableSlotRequest;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.ExamTimetableService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExamTimetableController {

    private final ExamTimetableService examTimetableService;

    public ExamTimetableController(ExamTimetableService examTimetableService) {
        this.examTimetableService = examTimetableService;
    }

    @GetMapping("/api/classes/{classId}/exam-timetable")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<ExamTimetableSlotResponse> listForClass(
        @PathVariable Long classId,
        @RequestParam Long termId,
        @RequestParam(defaultValue = "true") boolean includeUnpublished) {
        return examTimetableService.listForClass(
            classId, termId, includeUnpublished, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/api/classes/{classId}/exam-timetable")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamTimetableSlotResponse create(
        @PathVariable Long classId,
        @Valid @RequestBody CreateExamTimetableSlotRequest request) {
        return examTimetableService.create(classId, request, SecurityUtils.requireCurrentUser());
    }

    @PutMapping("/api/classes/{classId}/exam-timetable/{slotId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ExamTimetableSlotResponse update(
        @PathVariable Long classId,
        @PathVariable Long slotId,
        @Valid @RequestBody UpdateExamTimetableSlotRequest request) {
        return examTimetableService.update(classId, slotId, request, SecurityUtils.requireCurrentUser());
    }

    @DeleteMapping("/api/classes/{classId}/exam-timetable/{slotId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long classId, @PathVariable Long slotId) {
        examTimetableService.delete(classId, slotId, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/api/classes/{classId}/exam-timetable/publish")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public PublishExamTimetableResponse publish(
        @PathVariable Long classId,
        @RequestParam Long termId) {
        return examTimetableService.publish(classId, termId, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/api/students/{studentId}/exam-timetable")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','PARENT')")
    public List<ExamTimetableSlotResponse> listForStudent(
        @PathVariable Long studentId,
        @RequestParam Long termId) {
        return examTimetableService.listForStudent(studentId, termId, SecurityUtils.requireCurrentUser());
    }
}
