package com.example.backend.service;

import com.example.backend.entity.AcademicSession;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AcademicSessionRepository;
import org.springframework.stereotype.Service;

@Service
public class ActiveSessionService {

    private final AcademicSessionRepository academicSessionRepository;

    public ActiveSessionService(AcademicSessionRepository academicSessionRepository) {
        this.academicSessionRepository = academicSessionRepository;
    }

    public AcademicSession requireActiveSession() {
        return academicSessionRepository.findFirstByIsActiveTrue()
            .orElseThrow(() -> new ResourceNotFoundException("No active academic session configured"));
    }

    public Long requireActiveSessionId() {
        return requireActiveSession().getId();
    }
}
