package com.example.backend.service;

import com.example.backend.dto.classroom.CreateSubjectRequest;
import com.example.backend.dto.classroom.SubjectResponse;
import com.example.backend.entity.Subject;
import com.example.backend.exception.ConflictException;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.security.SecurityUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final AuditService auditService;

    public SubjectService(SubjectRepository subjectRepository, AuditService auditService) {
        this.subjectRepository = subjectRepository;
        this.auditService = auditService;
    }

    public Page<SubjectResponse> list(Pageable pageable) {
        return subjectRepository.findAll(pageable).map(SubjectResponse::from);
    }

    @Transactional
    public SubjectResponse create(CreateSubjectRequest request, SecurityUser actor) {
        if (subjectRepository.existsByName(request.name())) {
            throw new ConflictException("Subject name already exists");
        }
        Subject subject = new Subject();
        subject.setName(request.name());
        Subject saved = subjectRepository.save(subject);
        auditService.logAdminAction(actor, "CREATE", "subject", saved.getId(), saved.getName());
        return SubjectResponse.from(saved);
    }
}
