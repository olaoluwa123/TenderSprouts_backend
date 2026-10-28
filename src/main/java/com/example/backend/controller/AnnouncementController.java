package com.example.backend.controller;

import com.example.backend.dto.announcement.AnnouncementEmailBackfillResponse;
import com.example.backend.dto.announcement.AnnouncementResponse;
import com.example.backend.dto.announcement.CreateAnnouncementRequest;
import com.example.backend.dto.announcement.UpdateAnnouncementRequest;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.AnnouncementService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AnnouncementResponse> list() {
        return announcementService.list();
    }

    @GetMapping("/inbox")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public List<AnnouncementResponse> inbox() {
        return announcementService.inbox(SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public List<AnnouncementResponse> mine() {
        return announcementService.mine(SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/backfill-emails")
    @PreAuthorize("hasRole('ADMIN')")
    public AnnouncementEmailBackfillResponse backfillEmails() {
        return announcementService.backfillEmails(SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AnnouncementResponse get(@PathVariable Long id) {
        return announcementService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementResponse create(@Valid @RequestBody CreateAnnouncementRequest request) {
        return announcementService.create(request, SecurityUtils.requireCurrentUser());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public AnnouncementResponse update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateAnnouncementRequest request) {
        return announcementService.update(id, request, SecurityUtils.requireCurrentUser());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        announcementService.delete(id, SecurityUtils.requireCurrentUser());
    }
}
