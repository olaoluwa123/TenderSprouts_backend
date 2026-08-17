package com.example.backend.service;

import com.example.backend.dto.classroom.CreateSubjectRequest;
import com.example.backend.dto.classroom.SubjectProfileResponse;
import com.example.backend.dto.classroom.SubjectProfileResponse.ClassAssignment;
import com.example.backend.dto.classroom.SubjectProfileResponse.TeacherAssignment;
import com.example.backend.dto.classroom.SubjectResponse;
import com.example.backend.dto.classroom.UpdateSubjectRequest;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Subject;
import com.example.backend.entity.Teacher;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ClassSubjectRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TeacherSubjectRepository;
import com.example.backend.security.SecurityUser;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final TeacherRepository teacherRepository;
    private final AuditService auditService;

    public SubjectService(
        SubjectRepository subjectRepository,
        ClassSubjectRepository classSubjectRepository,
        SchoolClassRepository schoolClassRepository,
        TeacherSubjectRepository teacherSubjectRepository,
        TeacherRepository teacherRepository,
        AuditService auditService) {
        this.subjectRepository = subjectRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.teacherSubjectRepository = teacherSubjectRepository;
        this.teacherRepository = teacherRepository;
        this.auditService = auditService;
    }

    public Page<SubjectResponse> list(Pageable pageable) {
        return subjectRepository.findAll(pageable).map(SubjectResponse::from);
    }

    public SubjectProfileResponse getProfile(Long id) {
        Subject subject = findSubject(id);
        Map<Long, String> classNames = schoolClassRepository.findAll().stream()
            .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName, (a, b) -> a));
        Map<Long, String> teacherNames = teacherRepository.findAll().stream()
            .collect(Collectors.toMap(Teacher::getId, Teacher::getFullName, (a, b) -> a));

        List<ClassAssignment> classes = classSubjectRepository.findBySubjectIdAndIsActiveTrue(id).stream()
            .map(cs -> new ClassAssignment(
                cs.getClassId(),
                classNames.getOrDefault(cs.getClassId(), "Class #" + cs.getClassId()),
                cs.getIsActive()))
            .toList();

        List<TeacherAssignment> teachers = teacherSubjectRepository.findBySubjectIdAndIsActiveTrue(id).stream()
            .map(ts -> new TeacherAssignment(
                ts.getTeacherId(),
                teacherNames.getOrDefault(ts.getTeacherId(), "Teacher #" + ts.getTeacherId()),
                ts.getClassId(),
                classNames.getOrDefault(ts.getClassId(), "Class #" + ts.getClassId())))
            .toList();

        return new SubjectProfileResponse(
            subject.getId(),
            subject.getName(),
            subject.getIsActive(),
            classes,
            teachers);
    }

    @Transactional
    public SubjectResponse create(CreateSubjectRequest request, SecurityUser actor) {
        if (subjectRepository.existsByName(request.name())) {
            throw new ConflictException("Subject name already exists");
        }
        Subject subject = new Subject();
        subject.setName(request.name());
        subject.setIsActive(true);
        Subject saved = subjectRepository.save(subject);
        auditService.logAdminAction(actor, "CREATE", "subject", saved.getId(), saved.getName());
        return SubjectResponse.from(saved);
    }

    @Transactional
    public SubjectResponse update(Long id, UpdateSubjectRequest request, SecurityUser actor) {
        Subject subject = findSubject(id);
        if (request.name() != null && !request.name().isBlank()) {
            String name = request.name().trim();
            if (!name.equalsIgnoreCase(subject.getName()) && subjectRepository.existsByName(name)) {
                throw new ConflictException("Subject name already exists");
            }
            subject.setName(name);
        }
        if (request.isActive() != null) {
            subject.setIsActive(request.isActive());
        }
        Subject saved = subjectRepository.save(subject);
        auditService.logAdminAction(actor, "UPDATE", "subject", saved.getId(), saved.getName());
        return SubjectResponse.from(saved);
    }

    @Transactional
    public void softDelete(Long id, SecurityUser actor) {
        Subject subject = findSubject(id);
        subject.setIsActive(false);
        subjectRepository.save(subject);
        auditService.logAdminAction(actor, "DEACTIVATE", "subject", subject.getId(), subject.getName());
    }

    private Subject findSubject(Long id) {
        return subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
    }
}
