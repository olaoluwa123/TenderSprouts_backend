package com.example.backend.controller;

import com.example.backend.dto.calendar.CalendarEventResponse;
import com.example.backend.dto.calendar.CreateCalendarEventRequest;
import com.example.backend.dto.calendar.UpdateCalendarEventRequest;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.CalendarService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar/events")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public List<CalendarEventResponse> list(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return calendarService.list(from, to);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public CalendarEventResponse get(@PathVariable Long id) {
        return calendarService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CalendarEventResponse create(@Valid @RequestBody CreateCalendarEventRequest request) {
        return calendarService.create(request, SecurityUtils.requireCurrentUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CalendarEventResponse update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateCalendarEventRequest request) {
        return calendarService.update(id, request, SecurityUtils.requireCurrentUser());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        calendarService.delete(id, SecurityUtils.requireCurrentUser());
    }
}
