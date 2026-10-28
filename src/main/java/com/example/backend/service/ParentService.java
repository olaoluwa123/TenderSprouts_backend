package com.example.backend.service;

import com.example.backend.dto.parent.CreateParentRequest;
import com.example.backend.dto.parent.LinkParentStudentRequest;
import com.example.backend.dto.parent.LinkedStudentSummary;
import com.example.backend.dto.parent.OnboardParentForStudentRequest;
import com.example.backend.dto.parent.ParentListResponse;
import com.example.backend.dto.parent.ParentMeResponse;
import com.example.backend.dto.parent.ParentResponse;
import com.example.backend.dto.parent.UpdateParentMeRequest;
import com.example.backend.dto.student.StudentResponse;
import com.example.backend.entity.Parent;
import com.example.backend.entity.ParentStudent;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Student;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.ClassGroup;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.SecurityUser;
import com.example.backend.util.TemporaryPasswordGenerator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParentService {

    private final ParentRepository parentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final PasswordEncoder passwordEncoder;
    private final PortalUserOnboardingService portalUserOnboardingService;
    private final AuditService auditService;
    private final AuthorizationService authorizationService;

    public ParentService(
        ParentRepository parentRepository,
        ParentStudentRepository parentStudentRepository,
        UserRepository userRepository,
        StudentRepository studentRepository,
        SchoolClassRepository schoolClassRepository,
        PasswordEncoder passwordEncoder,
        PortalUserOnboardingService portalUserOnboardingService,
        AuditService auditService,
        AuthorizationService authorizationService) {
        this.parentRepository = parentRepository;
        this.parentStudentRepository = parentStudentRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.passwordEncoder = passwordEncoder;
        this.portalUserOnboardingService = portalUserOnboardingService;
        this.auditService = auditService;
        this.authorizationService = authorizationService;
    }

    public ParentMeResponse getMe(SecurityUser actor) {
        if (actor.getRole() != User.Role.PARENT || actor.getProfileId() == null) {
            throw new ForbiddenException("Parent profile required");
        }
        Parent parent = parentRepository.findById(actor.getProfileId())
            .orElseThrow(() -> new ResourceNotFoundException("Parent not found"));
        User user = userRepository.findById(parent.getUserId()).orElseThrow();
        List<Student> students = parentStudentRepository.findByParentId(parent.getId()).stream()
            .map(ParentStudent::getStudentId)
            .map(studentRepository::findById)
            .flatMap(java.util.Optional::stream)
            .toList();
        Set<Long> classIds = students.stream()
            .map(Student::getClassId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, ClassGroup> classGroups = schoolClassRepository.findAllById(classIds).stream()
            .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getClassGroup, (left, right) -> left));
        List<StudentResponse> children = students.stream()
            .map(student -> StudentResponse.from(
                student,
                false,
                student.getClassId() == null ? null : classGroups.get(student.getClassId())))
            .toList();
        return new ParentMeResponse(
            parent.getId(),
            user.getId(),
            user.getEmail(),
            parent.getFullName(),
            parent.getPhone(),
            children);
    }

    @Transactional
    public ParentMeResponse updateMe(UpdateParentMeRequest request, SecurityUser actor) {
        if (actor.getRole() != User.Role.PARENT || actor.getProfileId() == null) {
            throw new ForbiddenException("Parent profile required");
        }
        Parent parent = parentRepository.findById(actor.getProfileId())
            .orElseThrow(() -> new ResourceNotFoundException("Parent not found"));
        if (request.fullName() != null && !request.fullName().isBlank()) {
            parent.setFullName(request.fullName().trim());
        }
        if (request.phone() != null) {
            String phone = request.phone().trim();
            parent.setPhone(phone.isEmpty() ? null : phone);
        }
        parentRepository.save(parent);
        auditService.logAdminAction(actor, "UPDATE", "parent_profile", parent.getId(), parent.getFullName());
        return getMe(actor);
    }

    public Page<ParentListResponse> list(SecurityUser actor, Long classId, Pageable pageable) {
        if (actor.getRole() == User.Role.ADMIN) {
            if (classId != null) {
                Set<Long> classIds = Set.of(classId);
                return parentRepository.findByStudentsInClassIds(classIds, pageable)
                    .map(parent -> toListResponse(parent, classIds));
            }
            return parentRepository.findAll(pageable)
                .map(parent -> toListResponse(parent, null));
        }
        if (actor.getRole() == User.Role.TEACHER) {
            Set<Long> classIds = authorizationService.getAssignedClassIds(actor);
            if (classIds.isEmpty()) {
                return Page.empty(pageable);
            }
            final Set<Long> filterClassIds;
            if (classId != null) {
                authorizationService.requireTeacherForClass(actor, classId);
                filterClassIds = Set.of(classId);
            } else {
                filterClassIds = classIds;
            }
            return parentRepository.findByStudentsInClassIds(filterClassIds, pageable)
                .map(parent -> toListResponse(parent, filterClassIds));
        }
        throw new ForbiddenException("Not allowed to list parents");
    }

    public ParentListResponse getById(Long id, SecurityUser actor) {
        Parent parent = findParent(id);
        if (actor.getRole() == User.Role.ADMIN) {
            return toListResponse(parent, null);
        }
        if (actor.getRole() == User.Role.TEACHER) {
            authorizationService.requireTeacherAccessToParent(actor, parent.getId());
            return toListResponse(parent, authorizationService.getAssignedClassIds(actor));
        }
        throw new ForbiddenException("Not allowed to view parent");
    }

    @Transactional
    public ParentResponse create(CreateParentRequest request, SecurityUser actor) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("Email already registered");
        }
        String plainPassword = TemporaryPasswordGenerator.generate();
        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(plainPassword));
        user.setRole(User.Role.PARENT);
        user.setIsActive(true);
        user.setMustChangePassword(true);
        user = userRepository.save(user);

        Parent parent = new Parent();
        parent.setUserId(user.getId());
        parent.setFullName(request.fullName());
        parent.setPhone(request.phone());
        parent.setAddress(blankToNull(request.address()));
        parent.setIsActive(true);
        parent = parentRepository.save(parent);

        portalUserOnboardingService.sendWelcomeCredentials(
            user,
            parent.getFullName(),
            "parent",
            plainPassword,
            parent.getPhone());

        auditService.logAdminAction(actor, "CREATE", "parent", parent.getId(), parent.getFullName());
        return ParentResponse.from(parent, user.getEmail());
    }

    @Transactional
    public ParentResponse onboardForStudent(
        Long studentId,
        OnboardParentForStudentRequest request,
        SecurityUser actor) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found");
        }
        PortalUserOnboardingService.OnboardedUser onboarded =
            portalUserOnboardingService.createUserWithTemporaryPassword(request.email(), User.Role.PARENT);
        User user = onboarded.user();

        Parent parent = new Parent();
        parent.setUserId(user.getId());
        parent.setFullName(request.fullName());
        parent.setPhone(request.phone());
        parent.setIsActive(true);
        parent = parentRepository.save(parent);

        ParentStudent link = new ParentStudent();
        link.setParentId(parent.getId());
        link.setStudentId(studentId);
        parentStudentRepository.save(link);

        portalUserOnboardingService.sendWelcomeCredentials(
            user,
            parent.getFullName(),
            "parent",
            onboarded.plainPassword(),
            parent.getPhone());

        auditService.logAdminAction(actor, "ONBOARD_PARENT", "parent", parent.getId(),
            "studentId=" + studentId);
        return ParentResponse.from(parent, user.getEmail());
    }

    @Transactional
    public ParentOnboardResult onboardOrLinkForStudent(
        Long studentId,
        OnboardParentForStudentRequest request,
        SecurityUser actor) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found");
        }
        java.util.Optional<User> existingUser = userRepository.findByEmail(request.email());
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.getRole() != User.Role.PARENT) {
                throw new ConflictException("Email already registered to a non-parent account");
            }
            Parent parent = parentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent profile not found for email"));
            if (parentStudentRepository.existsByParentIdAndStudentId(parent.getId(), studentId)) {
                throw new ConflictException("Student already linked to this parent");
            }
            ParentStudent link = new ParentStudent();
            link.setParentId(parent.getId());
            link.setStudentId(studentId);
            parentStudentRepository.save(link);
            auditService.logAdminAction(actor, "LINK", "parent_student", parent.getId(),
                "studentId=" + studentId);
            return new ParentOnboardResult(ParentResponse.from(parent, user.getEmail()), true);
        }
        ParentResponse created = onboardForStudent(studentId, request, actor);
        return new ParentOnboardResult(created, false);
    }

    public record ParentOnboardResult(ParentResponse parent, boolean linkedExisting) {
    }

    @Transactional
    public void linkStudent(Long parentId, LinkParentStudentRequest request, SecurityUser actor) {
        Parent parent = findParent(parentId);
        if (!studentRepository.existsById(request.studentId())) {
            throw new ResourceNotFoundException("Student not found");
        }
        if (parentStudentRepository.existsByParentIdAndStudentId(parentId, request.studentId())) {
            throw new ConflictException("Student already linked to this parent");
        }
        ParentStudent link = new ParentStudent();
        link.setParentId(parent.getId());
        link.setStudentId(request.studentId());
        parentStudentRepository.save(link);
        auditService.logAdminAction(actor, "LINK", "parent_student", parentId,
            "studentId=" + request.studentId());
    }

    @Transactional
    public void unlinkStudent(Long parentId, Long studentId, SecurityUser actor) {
        findParent(parentId);
        ParentStudent link = parentStudentRepository.findByParentId(parentId).stream()
            .filter(ps -> ps.getStudentId().equals(studentId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Link not found"));
        parentStudentRepository.delete(link);
        auditService.logAdminAction(actor, "UNLINK", "parent_student", parentId, "studentId=" + studentId);
    }

    private Parent findParent(Long id) {
        return parentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Parent not found"));
    }

    private ParentListResponse toListResponse(Parent parent, Set<Long> classIds) {
        var user = userRepository.findById(parent.getUserId()).orElse(null);
        String email = user != null ? user.getEmail() : null;
        Boolean loginActive = user != null ? user.getIsActive() : null;
        List<LinkedStudentSummary> students = parentStudentRepository.findByParentId(parent.getId()).stream()
            .map(ParentStudent::getStudentId)
            .map(studentRepository::findById)
            .filter(java.util.Optional::isPresent)
            .map(java.util.Optional::get)
            .filter(student -> classIds == null
                || (student.getClassId() != null && classIds.contains(student.getClassId())))
            .map(LinkedStudentSummary::from)
            .toList();
        return ParentListResponse.from(parent, email, loginActive, students);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
