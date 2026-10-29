package com.example.backend.controller;

import com.example.backend.dto.calendar.CalendarPosterResponse;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.CalendarPosterService;
import com.example.backend.service.CalendarPosterService.PosterFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/calendar/poster")
public class CalendarPosterController {

    private final CalendarPosterService calendarPosterService;

    public CalendarPosterController(CalendarPosterService calendarPosterService) {
        this.calendarPosterService = calendarPosterService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public CalendarPosterResponse metadata() {
        return calendarPosterService.metadata();
    }

    @GetMapping("/file")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<byte[]> file() {
        PosterFile file = calendarPosterService.getFile();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(file.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.filename() + "\"")
            .header(HttpHeaders.CACHE_CONTROL, "private, max-age=60")
            .body(file.bytes());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public CalendarPosterResponse upload(@RequestParam("file") MultipartFile file) {
        return calendarPosterService.upload(file, SecurityUtils.requireCurrentUser());
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete() {
        calendarPosterService.delete(SecurityUtils.requireCurrentUser());
    }
}
