package com.example.backend.service;

import com.example.backend.dto.session.AcademicSessionResponse;
import com.example.backend.dto.session.CreateSessionRequest;
import com.example.backend.dto.session.TermResponse;
import com.example.backend.entity.AcademicSession;
import com.example.backend.entity.Term;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AcademicSessionRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.security.SecurityUser;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicSessionService {

    private static final List<String> DEFAULT_TERM_NAMES = List.of(
        "First Term", "Second Term", "Third Term");

    private final AcademicSessionRepository academicSessionRepository;
    private final TermRepository termRepository;
    private final ActiveSessionService activeSessionService;
    private final AuditService auditService;

    public AcademicSessionService(
        AcademicSessionRepository academicSessionRepository,
        TermRepository termRepository,
        ActiveSessionService activeSessionService,
        AuditService auditService) {
        this.academicSessionRepository = academicSessionRepository;
        this.termRepository = termRepository;
        this.activeSessionService = activeSessionService;
        this.auditService = auditService;
    }

    public Page<AcademicSessionResponse> list(Pageable pageable) {
        return academicSessionRepository.findAll(pageable).map(AcademicSessionResponse::from);
    }

    public AcademicSessionResponse getActive() {
        return AcademicSessionResponse.from(
            academicSessionRepository.findFirstByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active academic session")));
    }

    @Transactional
    public AcademicSessionResponse create(CreateSessionRequest request, SecurityUser actor) {
        AcademicSession session = new AcademicSession();
        session.setName(request.name());
        session.setStartDate(request.startDate());
        session.setEndDate(request.endDate());
        session.setIsActive(false);
        AcademicSession saved = academicSessionRepository.save(session);
        createDefaultTerms(saved.getId());
        auditService.logAdminAction(actor, "CREATE", "academic_session", saved.getId(), saved.getName());
        return AcademicSessionResponse.from(saved);
    }

    public void ensureDefaultTermsIfMissing(Long sessionId) {
        if (!academicSessionRepository.existsById(sessionId)) {
            return;
        }
        List<Term> existing = termRepository.findBySessionIdOrderByNameAsc(sessionId);
        Set<String> existingNames = existing.stream()
            .map(Term::getName)
            .collect(Collectors.toSet());
        boolean hasActiveTerm = existing.stream().anyMatch(t -> Boolean.TRUE.equals(t.getIsActive()));
        for (int i = 0; i < DEFAULT_TERM_NAMES.size(); i++) {
            String name = DEFAULT_TERM_NAMES.get(i);
            if (!existingNames.contains(name)) {
                boolean shouldActivate = !hasActiveTerm && i == 0;
                if (shouldActivate) {
                    deactivateAllTerms();
                }
                saveTerm(sessionId, name, shouldActivate);
                if (shouldActivate) {
                    hasActiveTerm = true;
                }
            }
        }
    }

    private void createDefaultTerms(Long sessionId) {
        saveTerm(sessionId, DEFAULT_TERM_NAMES.get(0), false);
        saveTerm(sessionId, DEFAULT_TERM_NAMES.get(1), false);
        saveTerm(sessionId, DEFAULT_TERM_NAMES.get(2), false);
    }

    private void saveTerm(Long sessionId, String name, boolean active) {
        Term term = new Term();
        term.setSessionId(sessionId);
        term.setName(name);
        term.setIsActive(active);
        termRepository.save(term);
    }

    @Transactional
    public AcademicSessionResponse activate(Long sessionId, SecurityUser actor) {
        AcademicSession session = academicSessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        ensureDefaultTermsIfMissing(sessionId);
        academicSessionRepository.findAll().forEach(s -> {
            s.setIsActive(false);
            academicSessionRepository.save(s);
        });
        session.setIsActive(true);
        AcademicSession saved = academicSessionRepository.save(session);
        deactivateAllTerms();
        activateFirstTermInSession(sessionId);
        auditService.logAdminAction(actor, "ACTIVATE", "academic_session", saved.getId(), saved.getName());
        return AcademicSessionResponse.from(saved);
    }

    @Transactional
    public TermResponse activateTerm(Long sessionId, Long termId, SecurityUser actor) {
        AcademicSession activeSession = activeSessionService.requireActiveSession();
        if (!activeSession.getId().equals(sessionId)) {
            throw new ConflictException("Terms can only be activated within the active academic session");
        }
        Term term = termRepository.findById(termId)
            .orElseThrow(() -> new ResourceNotFoundException("Term not found"));
        if (!term.getSessionId().equals(sessionId)) {
            throw new ConflictException("Term does not belong to this session");
        }
        deactivateAllTerms();
        term.setIsActive(true);
        Term saved = termRepository.save(term);
        auditService.logAdminAction(actor, "ACTIVATE", "term", saved.getId(), saved.getName());
        return TermResponse.from(saved);
    }

    private void deactivateAllTerms() {
        termRepository.findAll().forEach(t -> {
            t.setIsActive(false);
            termRepository.save(t);
        });
    }

    private void activateFirstTermInSession(Long sessionId) {
        List<Term> terms = termRepository.findBySessionIdOrderByNameAsc(sessionId);
        if (terms.isEmpty()) {
            return;
        }
        Term first = terms.stream()
            .filter(t -> "First Term".equals(t.getName()))
            .findFirst()
            .orElse(terms.get(0));
        first.setIsActive(true);
        termRepository.save(first);
    }

    public List<TermResponse> listTerms(Long sessionId) {
        if (!academicSessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Session not found");
        }
        ensureDefaultTermsIfMissing(sessionId);
        return termRepository.findBySessionIdOrderByNameAsc(sessionId).stream()
            .map(TermResponse::from)
            .toList();
    }
}
