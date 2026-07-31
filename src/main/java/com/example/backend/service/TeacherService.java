package com.example.backend.service;

import com.example.backend.dto.teacher.OnboardTeacherRequest;
import com.example.backend.dto.teacher.TeacherResponse;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.User;
import com.example.backend.security.SecurityUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.backend.repository.TeacherRepository;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final PortalUserOnboardingService portalUserOnboardingService;
    private final AuditService auditService;

    public TeacherService(
        TeacherRepository teacherRepository,
        PortalUserOnboardingService portalUserOnboardingService,
        AuditService auditService) {
        this.teacherRepository = teacherRepository;
        this.portalUserOnboardingService = portalUserOnboardingService;
        this.auditService = auditService;
    }

    @Transactional
    public TeacherResponse onboard(OnboardTeacherRequest request, SecurityUser actor) {
        PortalUserOnboardingService.OnboardedUser onboarded =
            portalUserOnboardingService.createUserWithTemporaryPassword(request.email(), User.Role.TEACHER);

        Teacher teacher = new Teacher();
        teacher.setUserId(onboarded.user().getId());
        teacher.setFullName(request.fullName());
        teacher.setPhone(request.phone());
        teacher.setIsActive(true);
        teacher = teacherRepository.save(teacher);

        portalUserOnboardingService.sendWelcomeCredentialsEmail(
            onboarded.user(),
            request.fullName(),
            "teacher",
            onboarded.plainPassword());

        auditService.logAdminAction(actor, "ONBOARD_TEACHER", "teacher", teacher.getId(), teacher.getFullName());
        return TeacherResponse.from(teacher, onboarded.user().getEmail());
    }
}
