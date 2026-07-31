package com.example.backend.service;

import com.example.backend.entity.AcademicSession;
import com.example.backend.entity.Term;
import com.example.backend.entity.User;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.TermRepository;
import com.example.backend.security.SecurityUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActiveTermService {

    private static final String FIRST_TERM_NAME = "First Term";

    private final TermRepository termRepository;
    private final ActiveSessionService activeSessionService;

    public ActiveTermService(TermRepository termRepository, ActiveSessionService activeSessionService) {
        this.termRepository = termRepository;
        this.activeSessionService = activeSessionService;
    }

    public Term requireActiveTerm() {
        AcademicSession session = activeSessionService.requireActiveSession();
        return termRepository.findFirstBySessionIdAndIsActiveTrue(session.getId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "No active term for the current academic session. An admin must activate a term under Sessions."));
    }

    public Long requireActiveTermId() {
        return requireActiveTerm().getId();
    }

    public void requireMatchesActiveTermForWrite(Long termId, SecurityUser actor) {
        if (actor.getRole() == User.Role.ADMIN) {
            return;
        }
        if (!requireActiveTermId().equals(termId)) {
            throw new ConflictException("This action is only allowed for the active term");
        }
    }

    /**
     * Ensures exactly one active term exists and it belongs to the active academic session.
     */
    @Transactional
    public void alignActiveTermWithActiveSession() {
        AcademicSession session = activeSessionService.requireActiveSession();
        Long sessionId = session.getId();

        var activeInSession = termRepository.findFirstBySessionIdAndIsActiveTrue(sessionId);
        if (activeInSession.isPresent()) {
            deactivateAllExcept(activeInSession.get().getId());
            return;
        }

        deactivateAllTerms();
        activateFirstTermInSession(sessionId);
    }

    private void deactivateAllExcept(Long keepActiveId) {
        termRepository.findAll().forEach(term -> {
            boolean shouldBeActive = term.getId().equals(keepActiveId);
            if (!Boolean.valueOf(shouldBeActive).equals(term.getIsActive())) {
                term.setIsActive(shouldBeActive);
                termRepository.save(term);
            }
        });
    }

    private void deactivateAllTerms() {
        termRepository.findAll().forEach(term -> {
            if (Boolean.TRUE.equals(term.getIsActive())) {
                term.setIsActive(false);
                termRepository.save(term);
            }
        });
    }

    private void activateFirstTermInSession(Long sessionId) {
        List<Term> terms = termRepository.findBySessionIdOrderByNameAsc(sessionId);
        if (terms.isEmpty()) {
            return;
        }
        Term first = terms.stream()
            .filter(term -> FIRST_TERM_NAME.equals(term.getName()))
            .findFirst()
            .orElse(terms.get(0));
        first.setIsActive(true);
        termRepository.save(first);
    }
}
