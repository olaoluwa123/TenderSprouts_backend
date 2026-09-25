package com.example.backend.service;

import com.example.backend.dto.student.BulkPromoteRequest;
import com.example.backend.dto.student.BulkPromoteResponse;
import com.example.backend.dto.student.BulkPromoteResultItem;
import com.example.backend.dto.parent.LinkParentStudentRequest;
import com.example.backend.dto.parent.OnboardParentForStudentRequest;
import com.example.backend.dto.parent.ParentResponse;
import com.example.backend.dto.student.CreateEnrollmentRequest;
import com.example.backend.dto.student.CreateStudentRequest;
import com.example.backend.dto.student.EnrollmentResponse;
import com.example.backend.dto.student.OnboardStudentWithParentRequest;
import com.example.backend.dto.student.OnboardStudentWithParentResponse;
import com.example.backend.dto.student.StudentResponse;
import com.example.backend.dto.student.UpdateStudentRequest;
import com.example.backend.entity.ParentStudent;
import com.example.backend.entity.Student;
import com.example.backend.entity.StudentEnrollment;
import com.example.backend.entity.AcademicSession;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.EnrollmentStatus;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AcademicSessionRepository;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentEnrollmentRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.security.SecurityUser;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final ActiveSessionService activeSessionService;
    private final AuditService auditService;
    private final ParentService parentService;
    private final AuthorizationService authorizationService;

    public StudentService(
        StudentRepository studentRepository,
        StudentEnrollmentRepository studentEnrollmentRepository,
        SchoolClassRepository schoolClassRepository,
        AcademicSessionRepository academicSessionRepository,
        ParentStudentRepository parentStudentRepository,
        ActiveSessionService activeSessionService,
        AuditService auditService,
        ParentService parentService,
        AuthorizationService authorizationService) {
        this.studentRepository = studentRepository;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.academicSessionRepository = academicSessionRepository;
        this.parentStudentRepository = parentStudentRepository;
        this.activeSessionService = activeSessionService;
        this.auditService = auditService;
        this.parentService = parentService;
        this.authorizationService = authorizationService;
    }

    public Page<StudentResponse> list(
        Long classId,
        Long sessionId,
        String search,
        Pageable pageable,
        SecurityUser actor) {
        Page<Student> page;
        boolean filteredByParent = actor != null && actor.getRole() == User.Role.PARENT;
        boolean teacher = actor != null && actor.getRole() == User.Role.TEACHER;
        if (filteredByParent) {
            List<Long> childIds = actor.getProfileId() == null
                ? List.of()
                : parentStudentRepository.findByParentId(actor.getProfileId()).stream()
                    .map(ParentStudent::getStudentId)
                    .toList();
            page = childIds.isEmpty()
                ? new PageImpl<>(List.of(), pageable, 0)
                : studentRepository.findByIdIn(childIds, pageable);
        } else if (teacher) {
            page = listForTeacher(actor, classId, sessionId, search, pageable);
        } else if (sessionId != null) {
            page = studentRepository.findByActiveEnrollment(sessionId, classId, pageable);
        } else if (search != null && !search.isBlank()) {
            page = studentRepository.search(classId, search.trim(), pageable);
        } else if (classId != null) {
            page = studentRepository.findByClassId(classId, pageable);
        } else {
            page = studentRepository.findAll(pageable);
        }
        return mapWithParentLinks(page);
    }

    private Page<Student> listForTeacher(
        SecurityUser actor,
        Long classId,
        Long sessionId,
        String search,
        Pageable pageable) {
        if (classId != null) {
            authorizationService.requireTeacherForClass(actor, classId);
            if (sessionId != null) {
                return studentRepository.findByActiveEnrollment(sessionId, classId, pageable);
            }
            if (search != null && !search.isBlank()) {
                return studentRepository.search(classId, search.trim(), pageable);
            }
            return studentRepository.findByClassId(classId, pageable);
        }
        Set<Long> assigned = authorizationService.getAssignedClassIds(actor);
        if (assigned.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        if (sessionId != null) {
            return studentRepository.findByActiveEnrollmentInClasses(sessionId, assigned, pageable);
        }
        if (search != null && !search.isBlank()) {
            return studentRepository.searchInClasses(assigned, search.trim(), pageable);
        }
        return studentRepository.findByClassIdIn(assigned, pageable);
    }

    public StudentResponse getById(Long id) {
        Student student = findStudent(id);
        return StudentResponse.from(student, parentStudentRepository.existsByStudentId(id));
    }

    private Page<StudentResponse> mapWithParentLinks(Page<Student> page) {
        List<Long> ids = page.getContent().stream().map(Student::getId).toList();
        Set<Long> linked = ids.isEmpty()
            ? Set.of()
            : new HashSet<>(parentStudentRepository.findLinkedStudentIds(ids));
        return page.map(student -> StudentResponse.from(student, linked.contains(student.getId())));
    }

    @Transactional
    public StudentResponse create(CreateStudentRequest request, SecurityUser actor) {
        if (studentRepository.existsByAdmissionNumber(request.admissionNumber())) {
            throw new ConflictException("Admission number already exists");
        }
        if (!schoolClassRepository.existsById(request.classId())) {
            throw new ResourceNotFoundException("Class not found");
        }
        Student student = new Student();
        student.setFirstName(request.firstName());
        student.setLastName(request.lastName());
        student.setDateOfBirth(request.dateOfBirth());
        student.setAdmissionNumber(request.admissionNumber());
        student.setGender(request.gender());
        student.setClassId(request.classId());
        student.setHeight(blankToNull(request.height()));
        student.setWeight(blankToNull(request.weight()));
        student.setIsActive(true);
        Student saved = studentRepository.save(student);

        Long sessionId = activeSessionService.requireActiveSessionId();
        StudentEnrollment enrollment = enrollInternal(saved.getId(), request.classId(), sessionId, LocalDate.now());
        saved.setCurrentEnrollmentId(enrollment.getId());
        saved.setClassId(request.classId());
        saved = studentRepository.save(saved);

        auditService.logAdminAction(actor, "CREATE", "student", saved.getId(), saved.getAdmissionNumber());
        return StudentResponse.from(saved);
    }

    @Transactional
    public OnboardStudentWithParentResponse onboardWithParent(
        OnboardStudentWithParentRequest request,
        SecurityUser actor) {
        StudentResponse student = create(
            new CreateStudentRequest(
                request.firstName(),
                request.lastName(),
                request.dateOfBirth(),
                request.admissionNumber(),
                request.gender(),
                request.classId(),
                request.height(),
                request.weight()),
            actor);
        ParentResponse parent = parentService.onboardForStudent(
            student.id(),
            new OnboardParentForStudentRequest(
                request.parentEmail(),
                request.parentFullName(),
                request.parentPhone()),
            actor);
        return new OnboardStudentWithParentResponse(student, parent);
    }

    @Transactional
    public StudentResponse createForParent(Long parentId, CreateStudentRequest request, SecurityUser actor) {
        StudentResponse student = create(request, actor);
        parentService.linkStudent(parentId, new LinkParentStudentRequest(student.id()), actor);
        return student;
    }

    @Transactional
    public StudentResponse update(Long id, UpdateStudentRequest request, SecurityUser actor) {
        Student student = findStudent(id);
        if (request.firstName() != null) {
            student.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            student.setLastName(request.lastName());
        }
        if (request.dateOfBirth() != null) {
            student.setDateOfBirth(request.dateOfBirth());
        }
        if (request.gender() != null) {
            student.setGender(request.gender());
        }
        if (request.isActive() != null) {
            student.setIsActive(request.isActive());
        }
        if (request.address() != null) {
            student.setAddress(request.address());
        }
        if (request.medicalInformation() != null) {
            student.setMedicalInformation(request.medicalInformation());
        }
        if (request.height() != null) {
            student.setHeight(blankToNull(request.height()));
        }
        if (request.weight() != null) {
            student.setWeight(blankToNull(request.weight()));
        }
        Student saved = studentRepository.save(student);
        auditService.logAdminAction(actor, "UPDATE", "student", saved.getId(), null);
        return StudentResponse.from(saved);
    }

    @Transactional
    public EnrollmentResponse enroll(Long studentId, CreateEnrollmentRequest request, SecurityUser actor) {
        Student student = findStudent(studentId);
        Long sessionId = request.sessionId() != null
            ? request.sessionId()
            : activeSessionService.requireActiveSessionId();
        if (!schoolClassRepository.existsById(request.classId())) {
            throw new ResourceNotFoundException("Class not found");
        }
        if (studentEnrollmentRepository.findByStudentIdAndSessionId(studentId, sessionId).isPresent()) {
            throw new ConflictException("Student already enrolled for this session");
        }
        studentEnrollmentRepository.findByStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE)
            .ifPresent(existing -> {
                existing.setStatus(EnrollmentStatus.COMPLETED);
                existing.setEndDate(LocalDate.now());
                studentEnrollmentRepository.save(existing);
            });

        LocalDate start = request.startDate() != null ? request.startDate() : LocalDate.now();
        StudentEnrollment enrollment = enrollInternal(studentId, request.classId(), sessionId, start);
        student.setCurrentEnrollmentId(enrollment.getId());
        student.setClassId(request.classId());
        studentRepository.save(student);
        auditService.logAdminAction(actor, "ENROLL", "student_enrollment", enrollment.getId(), null);
        return EnrollmentResponse.from(enrollment);
    }

    public List<EnrollmentResponse> listEnrollments(Long studentId) {
        findStudent(studentId);
        return studentEnrollmentRepository.findByStudentIdOrderByStartDateDesc(studentId).stream()
            .map(EnrollmentResponse::from)
            .toList();
    }

    @Transactional
    public BulkPromoteResponse bulkPromote(BulkPromoteRequest request, SecurityUser actor) {
        schoolClassRepository.findById(request.sourceClassId())
            .orElseThrow(() -> new ResourceNotFoundException("Source class not found"));
        schoolClassRepository.findById(request.targetClassId())
            .orElseThrow(() -> new ResourceNotFoundException("Target class not found"));
        AcademicSession sourceSession = academicSessionRepository.findById(request.sourceSessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Source session not found"));
        AcademicSession targetSession = resolveNextSession(sourceSession);
        Long targetSessionId = targetSession.getId();

        List<StudentEnrollment> sourceEnrollments = studentEnrollmentRepository
            .findByClassIdAndSessionIdAndStatus(
                request.sourceClassId(), request.sourceSessionId(), EnrollmentStatus.ACTIVE);

        Set<Long> filterIds = request.studentIds() != null && !request.studentIds().isEmpty()
            ? new HashSet<>(request.studentIds())
            : null;

        List<BulkPromoteResultItem> results = new ArrayList<>();
        int promoted = 0;
        int skipped = 0;
        LocalDate startDate = targetSession.getStartDate() != null
            ? targetSession.getStartDate()
            : LocalDate.now();

        for (StudentEnrollment source : sourceEnrollments) {
            Long studentId = source.getStudentId();
            if (filterIds != null && !filterIds.contains(studentId)) {
                continue;
            }
            if (!Boolean.TRUE.equals(findStudent(studentId).getIsActive())) {
                results.add(new BulkPromoteResultItem(studentId, "SKIPPED", "Student is inactive"));
                skipped++;
                continue;
            }
            if (studentEnrollmentRepository.findByStudentIdAndSessionId(studentId, targetSessionId)
                .isPresent()) {
                results.add(new BulkPromoteResultItem(studentId, "SKIPPED", "Already enrolled in target session"));
                skipped++;
                continue;
            }
            try {
                studentEnrollmentRepository.findByStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE)
                    .ifPresent(existing -> {
                        existing.setStatus(EnrollmentStatus.COMPLETED);
                        existing.setEndDate(LocalDate.now());
                        studentEnrollmentRepository.save(existing);
                    });
                StudentEnrollment enrollment = enrollInternal(
                    studentId, request.targetClassId(), targetSessionId, startDate);
                Student student = findStudent(studentId);
                student.setCurrentEnrollmentId(enrollment.getId());
                student.setClassId(request.targetClassId());
                studentRepository.save(student);
                results.add(new BulkPromoteResultItem(studentId, "PROMOTED", null));
                promoted++;
            } catch (Exception ex) {
                results.add(new BulkPromoteResultItem(studentId, "SKIPPED", ex.getMessage()));
                skipped++;
            }
        }

        auditService.logAdminAction(actor, "BULK_PROMOTE", "student_enrollment", null,
            "promoted=" + promoted + ",skipped=" + skipped);
        return new BulkPromoteResponse(promoted, skipped, results);
    }

    private StudentEnrollment enrollInternal(Long studentId, Long classId, Long sessionId, LocalDate startDate) {
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setClassId(classId);
        enrollment.setSessionId(sessionId);
        enrollment.setStartDate(startDate);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        return studentEnrollmentRepository.save(enrollment);
    }

    private AcademicSession resolveNextSession(AcademicSession sourceSession) {
        if (sourceSession.getStartDate() != null) {
            return academicSessionRepository
                .findFirstByStartDateGreaterThanOrderByStartDateAsc(sourceSession.getStartDate())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "No academic session found after " + sourceSession.getName()));
        }
        return academicSessionRepository.findFirstByIdGreaterThanOrderByIdAsc(sourceSession.getId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "No academic session found after " + sourceSession.getName()));
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
