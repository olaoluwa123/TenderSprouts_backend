package com.example.backend.controller;

import com.example.backend.dto.session.AcademicSessionResponse;
import com.example.backend.dto.session.CreateSessionRequest;
import com.example.backend.dto.session.TermResponse;
import com.example.backend.security.SecurityUtils;
import com.example.backend.service.AcademicSessionService;
import com.example.backend.service.AuthorizationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class AcademicSessionController {

    private final AcademicSessionService academicSessionService;
    private final AuthorizationService authorizationService;

    public AcademicSessionController(
        AcademicSessionService academicSessionService,
        AuthorizationService authorizationService) {
        this.academicSessionService = academicSessionService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Page<AcademicSessionResponse> list(Pageable pageable) {
        return academicSessionService.list(pageable);
    }

    @GetMapping("/active")
    public AcademicSessionResponse getActive() {
        return academicSessionService.getActive();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicSessionResponse create(@Valid @RequestBody CreateSessionRequest request) {
        authorizationService.requireAdmin();
        return academicSessionService.create(request, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public AcademicSessionResponse activate(@PathVariable Long id) {
        authorizationService.requireAdmin();
        return academicSessionService.activate(id, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/{sessionId}/terms/{termId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public TermResponse activateTerm(@PathVariable Long sessionId, @PathVariable Long termId) {
        authorizationService.requireAdmin();
        return academicSessionService.activateTerm(sessionId, termId, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/{id}/terms")
    public List<TermResponse> listTerms(@PathVariable Long id) {
        return academicSessionService.listTerms(id);
    }
}
