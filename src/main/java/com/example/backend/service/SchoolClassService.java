package com.example.backend.service;

import com.example.backend.dto.classroom.AssignClassSubjectRequest;
import com.example.backend.dto.classroom.BulkAssignClassSubjectsRequest;
import com.example.backend.dto.classroom.BulkAssignClassSubjectsResponse;
import com.example.backend.dto.classroom.ClassSubjectResponse;
import com.example.backend.dto.classroom.CreateSchoolClassRequest;
import com.example.backend.dto.classroom.SchoolClassResponse;
import com.example.backend.entity.ClassSubject;
import com.example.backend.entity.SchoolClass;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ClassSubjectRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.security.SecurityUser;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final AuditService auditService;

    public SchoolClassService(
        SchoolClassRepository schoolClassRepository,
        ClassSubjectRepository classSubjectRepository,
        SubjectRepository subjectRepository,
        AuditService auditService) {
        this.schoolClassRepository = schoolClassRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.subjectRepository = subjectRepository;
        this.auditService = auditService;
    }

    public Page<SchoolClassResponse> list(Pageable pageable) {
        return schoolClassRepository.findAll(pageable).map(SchoolClassResponse::from);
    }

    @Transactional
    public SchoolClassResponse create(CreateSchoolClassRequest request, SecurityUser actor) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName(request.name());
        SchoolClass saved = schoolClassRepository.save(schoolClass);
        auditService.logAdminAction(actor, "CREATE", "class", saved.getId(), saved.getName());
        return SchoolClassResponse.from(saved);
    }

    public List<ClassSubjectResponse> listSubjects(Long classId) {
        requireClass(classId);
        return classSubjectRepository.findByClassId(classId).stream()
            .map(ClassSubjectResponse::from)
            .toList();
    }

    @Transactional
    public ClassSubjectResponse assignSubject(Long classId, AssignClassSubjectRequest request, SecurityUser actor) {
        requireClass(classId);
        if (!subjectRepository.existsById(request.subjectId())) {
            throw new ResourceNotFoundException("Subject not found");
        }
        if (classSubjectRepository.findByClassIdAndSubjectId(classId, request.subjectId()).isPresent()) {
            throw new ConflictException("Subject already assigned to class");
        }
        ClassSubject cs = new ClassSubject();
        cs.setClassId(classId);
        cs.setSubjectId(request.subjectId());
        ClassSubject saved = classSubjectRepository.save(cs);
        auditService.logAdminAction(actor, "ASSIGN", "class_subject", saved.getId(), null);
        return ClassSubjectResponse.from(saved);
    }

    @Transactional
    public BulkAssignClassSubjectsResponse bulkAssignSubjects(
        BulkAssignClassSubjectsRequest request,
        SecurityUser actor) {
        Set<Long> classIds = new HashSet<>(request.classIds());
        Set<Long> subjectIds = new HashSet<>(request.subjectIds());
        for (Long classId : classIds) {
            requireClass(classId);
        }
        for (Long subjectId : subjectIds) {
            if (!subjectRepository.existsById(subjectId)) {
                throw new ResourceNotFoundException("Subject not found: " + subjectId);
            }
        }

        int assigned = 0;
        int skipped = 0;
        for (Long classId : classIds) {
            for (Long subjectId : subjectIds) {
                if (classSubjectRepository.findByClassIdAndSubjectId(classId, subjectId).isPresent()) {
                    skipped++;
                    continue;
                }
                ClassSubject cs = new ClassSubject();
                cs.setClassId(classId);
                cs.setSubjectId(subjectId);
                cs.setIsActive(true);
                classSubjectRepository.save(cs);
                assigned++;
            }
        }

        auditService.logAdminAction(
            actor,
            "BULK_ASSIGN",
            "class_subject",
            null,
            "assigned=" + assigned + ",skipped=" + skipped);
        return new BulkAssignClassSubjectsResponse(assigned, skipped);
    }

    private void requireClass(Long classId) {
        if (!schoolClassRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found");
        }
    }
}
