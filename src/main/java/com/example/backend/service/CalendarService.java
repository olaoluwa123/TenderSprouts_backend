package com.example.backend.service;

import com.example.backend.dto.calendar.CalendarEventResponse;
import com.example.backend.dto.calendar.CreateCalendarEventRequest;
import com.example.backend.dto.calendar.UpdateCalendarEventRequest;
import com.example.backend.entity.SchoolCalendarEvent;
import com.example.backend.entity.SchoolClass;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.SchoolCalendarEventRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.security.SecurityUser;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalendarService {

    private final SchoolCalendarEventRepository eventRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;

    public CalendarService(
        SchoolCalendarEventRepository eventRepository,
        SchoolClassRepository schoolClassRepository,
        AuthorizationService authorizationService,
        AuditService auditService) {
        this.eventRepository = eventRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    public List<CalendarEventResponse> list(LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = to != null ? to : start.plusMonths(1).minusDays(1);
        Map<Long, String> classNames = classNameMap();
        return eventRepository.findOverlapping(start, end).stream()
            .map(e -> CalendarEventResponse.from(e, resolveClassName(e.getClassId(), classNames)))
            .toList();
    }

    public CalendarEventResponse get(Long id) {
        SchoolCalendarEvent event = find(id);
        return CalendarEventResponse.from(event, resolveClassName(event.getClassId(), classNameMap()));
    }

    @Transactional
    public CalendarEventResponse create(CreateCalendarEventRequest request, SecurityUser actor) {
        authorizationService.requireAdmin();
        if (request.classId() != null && !schoolClassRepository.existsById(request.classId())) {
            throw new ResourceNotFoundException("Class not found");
        }
        SchoolCalendarEvent event = new SchoolCalendarEvent();
        event.setTitle(request.title().trim());
        event.setEventType(request.eventType());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate() != null ? request.endDate() : request.startDate());
        event.setClassId(request.classId());
        event.setDescription(request.description());
        SchoolCalendarEvent saved = eventRepository.save(event);
        auditService.logAdminAction(actor, "CREATE", "calendar_event", saved.getId(), saved.getTitle());
        return CalendarEventResponse.from(saved, resolveClassName(saved.getClassId(), classNameMap()));
    }

    @Transactional
    public CalendarEventResponse update(Long id, UpdateCalendarEventRequest request, SecurityUser actor) {
        authorizationService.requireAdmin();
        SchoolCalendarEvent event = find(id);
        if (request.title() != null && !request.title().isBlank()) {
            event.setTitle(request.title().trim());
        }
        if (request.eventType() != null) {
            event.setEventType(request.eventType());
        }
        if (request.startDate() != null) {
            event.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            event.setEndDate(request.endDate());
        }
        if (request.classId() != null) {
            if (!schoolClassRepository.existsById(request.classId())) {
                throw new ResourceNotFoundException("Class not found");
            }
            event.setClassId(request.classId());
        }
        if (request.description() != null) {
            event.setDescription(request.description());
        }
        SchoolCalendarEvent saved = eventRepository.save(event);
        auditService.logAdminAction(actor, "UPDATE", "calendar_event", saved.getId(), saved.getTitle());
        return CalendarEventResponse.from(saved, resolveClassName(saved.getClassId(), classNameMap()));
    }

    @Transactional
    public void delete(Long id, SecurityUser actor) {
        authorizationService.requireAdmin();
        SchoolCalendarEvent event = find(id);
        eventRepository.delete(event);
        auditService.logAdminAction(actor, "DELETE", "calendar_event", id, event.getTitle());
    }

    private SchoolCalendarEvent find(Long id) {
        return eventRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Calendar event not found"));
    }

    private Map<Long, String> classNameMap() {
        return schoolClassRepository.findAll().stream()
            .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName, (a, b) -> a));
    }

    private String resolveClassName(Long classId, Map<Long, String> classNames) {
        if (classId == null) {
            return null;
        }
        return classNames.getOrDefault(classId, "Class #" + classId);
    }
}
