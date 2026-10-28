package com.example.backend.service;

import com.example.backend.config.AppFrontendProperties;
import com.example.backend.dto.announcement.AnnouncementEmailBackfillResponse;
import com.example.backend.dto.announcement.AnnouncementRecipientResponse;
import com.example.backend.dto.announcement.AnnouncementResponse;
import com.example.backend.dto.announcement.CreateAnnouncementRequest;
import com.example.backend.dto.announcement.UpdateAnnouncementRequest;
import com.example.backend.entity.Announcement;
import com.example.backend.entity.AnnouncementRecipient;
import com.example.backend.entity.Parent;
import com.example.backend.entity.ParentStudent;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Student;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.AnnouncementAudience;
import com.example.backend.entity.enums.AnnouncementRecipientType;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.AnnouncementRepository;
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.SecurityUser;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementService {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementService.class);

    private final AnnouncementRepository announcementRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final ParentNotifyService parentNotifyService;
    private final EmailService emailService;
    private final AppFrontendProperties frontendProperties;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;

    public AnnouncementService(
        AnnouncementRepository announcementRepository,
        SchoolClassRepository schoolClassRepository,
        ParentStudentRepository parentStudentRepository,
        StudentRepository studentRepository,
        ParentRepository parentRepository,
        TeacherRepository teacherRepository,
        UserRepository userRepository,
        ParentNotifyService parentNotifyService,
        EmailService emailService,
        AppFrontendProperties frontendProperties,
        AuthorizationService authorizationService,
        AuditService auditService) {
        this.announcementRepository = announcementRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.parentStudentRepository = parentStudentRepository;
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.parentNotifyService = parentNotifyService;
        this.emailService = emailService;
        this.frontendProperties = frontendProperties;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> list() {
        Map<Long, String> classNames = classNameMap();
        return announcementRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(a -> toResponse(a, classNames))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> mine(SecurityUser actor) {
        Map<Long, String> classNames = classNameMap();
        return announcementRepository.findByCreatedByOrderByCreatedAtDesc(actor.getUserId()).stream()
            .map(a -> toResponse(a, classNames))
            .toList();
    }

    @Transactional(readOnly = true)
    public AnnouncementResponse get(Long id) {
        return toResponse(find(id), classNameMap());
    }

    /**
     * One-shot: email every non-expired announcement to its audience using the "new"
     * template. Safe to call for catch-up after enabling announcement mail; calling again
     * will send duplicate emails.
     */
    @Transactional
    public AnnouncementEmailBackfillResponse backfillEmails(SecurityUser actor) {
        if (actor.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Only admins can backfill announcement emails");
        }
        List<Announcement> all = announcementRepository.findAllByOrderByCreatedAtDesc();
        LocalDateTime now = LocalDateTime.now();
        int notified = 0;
        int skippedExpired = 0;
        for (Announcement announcement : all) {
            if (announcement.getExpiresAt() != null && announcement.getExpiresAt().isBefore(now)) {
                skippedExpired++;
                continue;
            }
            notifyAudience(announcement, false);
            notified++;
        }
        auditService.logAdminAction(
            actor,
            "BACKFILL_EMAILS",
            "announcement",
            0L,
            "notified=" + notified + ",skippedExpired=" + skippedExpired);
        log.info(
            "Announcement email backfill complete: total={} notified={} skippedExpired={}",
            all.size(),
            notified,
            skippedExpired);
        return new AnnouncementEmailBackfillResponse(all.size(), notified, skippedExpired);
    }

    @Transactional
    public AnnouncementResponse create(CreateAnnouncementRequest request, SecurityUser actor) {
        enforceTeacherCreateAudience(actor, request.audience(), request.classId(), request.recipientIds());
        validateAudience(request.audience(), request.classId(), request.recipientIds());
        Announcement announcement = new Announcement();
        announcement.setTitle(request.title().trim());
        announcement.setContent(request.content().trim());
        announcement.setAudience(request.audience());
        announcement.setIsImportant(Boolean.TRUE.equals(request.isImportant()));
        announcement.setClassId(request.classId());
        announcement.setCreatedBy(actor.getUserId());
        announcement.setPublishedAt(request.publishedAt() != null ? request.publishedAt() : LocalDateTime.now());
        announcement.setExpiresAt(request.expiresAt());
        applyRecipients(announcement, request.audience(), request.recipientIds());
        Announcement saved = announcementRepository.save(announcement);
        auditService.logAdminAction(actor, "CREATE", "announcement", saved.getId(), saved.getTitle());
        notifyAudience(saved, false);
        return toResponse(saved, classNameMap());
    }

    @Transactional
    public AnnouncementResponse update(Long id, UpdateAnnouncementRequest request, SecurityUser actor) {
        Announcement announcement = find(id);
        requireOwnerOrAdmin(actor, announcement);
        if (request.title() != null && !request.title().isBlank()) {
            announcement.setTitle(request.title().trim());
        }
        if (request.content() != null && !request.content().isBlank()) {
            announcement.setContent(request.content().trim());
        }
        if (request.isImportant() != null) {
            announcement.setIsImportant(request.isImportant());
        }
        if (request.publishedAt() != null) {
            announcement.setPublishedAt(request.publishedAt());
        }
        if (request.expiresAt() != null) {
            announcement.setExpiresAt(request.expiresAt());
        }
        AnnouncementAudience audience = request.audience() != null ? request.audience() : announcement.getAudience();
        Long classId = request.classId() != null ? request.classId() : announcement.getClassId();
        List<Long> recipientIds = request.recipientIds() != null
            ? request.recipientIds()
            : announcement.getRecipients().stream().map(AnnouncementRecipient::getRecipientId).toList();
        if (request.audience() != null || request.recipientIds() != null || request.classId() != null) {
            if (actor.getRole() == User.Role.TEACHER) {
                enforceTeacherCreateAudience(actor, audience, classId, recipientIds);
            }
            validateAudience(audience, classId, recipientIds);
            if (request.audience() != null) {
                announcement.setAudience(request.audience());
            }
            if (request.classId() != null) {
                announcement.setClassId(request.classId());
            }
            if (request.recipientIds() != null || request.audience() != null) {
                announcement.clearRecipients();
                applyRecipients(announcement, audience, recipientIds);
            }
        }
        Announcement saved = announcementRepository.save(announcement);
        auditService.logAdminAction(actor, "UPDATE", "announcement", saved.getId(), saved.getTitle());
        notifyAudience(saved, true);
        return toResponse(saved, classNameMap());
    }

    @Transactional
    public void delete(Long id, SecurityUser actor) {
        Announcement announcement = find(id);
        requireOwnerOrAdmin(actor, announcement);
        announcementRepository.delete(announcement);
        auditService.logAdminAction(actor, "DELETE", "announcement", id, announcement.getTitle());
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> inbox(SecurityUser actor) {
        Map<Long, String> classNames = classNameMap();
        List<Announcement> items = switch (actor.getRole()) {
            case ADMIN -> announcementRepository.findTop50ByOrderByCreatedAtDesc();
            case TEACHER -> {
                Long teacherId = actor.getProfileId();
                if (teacherId == null) {
                    yield List.of();
                }
                yield announcementRepository.findTeacherInbox(teacherId);
            }
            case PARENT -> parentInbox(actor.getProfileId());
        };
        return items.stream().map(a -> toResponse(a, classNames)).toList();
    }

    private void notifyAudience(Announcement announcement, boolean updated) {
        try {
            AnnouncementAudience audience = announcement.getAudience();
            if (audience == null) {
                return;
            }
            switch (audience) {
                case ALL_PARENTS -> notifyAllParents(announcement, updated);
                case CLASS -> {
                    if (announcement.getClassId() != null) {
                        String portalUrl = parentAnnouncementsUrl();
                        parentNotifyService.notifyParentsForClass(
                            announcement.getClassId(),
                            (user, parent) -> sendParentAnnouncement(user, parent, announcement, portalUrl, updated));
                    }
                }
                case SPECIFIC_PARENTS -> notifySpecificParents(announcement, updated);
                case SPECIFIC_PUPILS -> {
                    List<Long> pupilIds = announcement.getRecipients().stream()
                        .filter(r -> r.getRecipientType() == AnnouncementRecipientType.PUPIL)
                        .map(AnnouncementRecipient::getRecipientId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
                    if (!pupilIds.isEmpty()) {
                        String portalUrl = parentAnnouncementsUrl();
                        parentNotifyService.notifyParentsForStudents(
                            pupilIds,
                            (user, parent) -> sendParentAnnouncement(user, parent, announcement, portalUrl, updated));
                    }
                }
                case TEACHERS -> notifyAllTeachers(announcement, updated);
            }
        } catch (RuntimeException ex) {
            log.warn(
                "Announcement email notify failed: announcementId={} audience={} updated={} message={}",
                announcement.getId(),
                announcement.getAudience(),
                updated,
                ex.getMessage());
        }
    }

    private void notifyAllParents(Announcement announcement, boolean updated) {
        String portalUrl = parentAnnouncementsUrl();
        for (Parent parent : parentRepository.findAll()) {
            if (!Boolean.TRUE.equals(parent.getIsActive())) {
                continue;
            }
            User user = userRepository.findById(parent.getUserId()).orElse(null);
            if (!isActiveEmailUser(user)) {
                continue;
            }
            sendParentAnnouncement(user, parent, announcement, portalUrl, updated);
        }
    }

    private void notifySpecificParents(Announcement announcement, boolean updated) {
        String portalUrl = parentAnnouncementsUrl();
        List<Long> parentIds = announcement.getRecipients().stream()
            .filter(r -> r.getRecipientType() == AnnouncementRecipientType.PARENT)
            .map(AnnouncementRecipient::getRecipientId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        for (Long parentId : parentIds) {
            Parent parent = parentRepository.findById(parentId).orElse(null);
            if (parent == null || !Boolean.TRUE.equals(parent.getIsActive())) {
                continue;
            }
            User user = userRepository.findById(parent.getUserId()).orElse(null);
            if (!isActiveEmailUser(user)) {
                continue;
            }
            sendParentAnnouncement(user, parent, announcement, portalUrl, updated);
        }
    }

    private void notifyAllTeachers(Announcement announcement, boolean updated) {
        String portalUrl = teacherAnnouncementsUrl();
        for (Teacher teacher : teacherRepository.findAll()) {
            if (!Boolean.TRUE.equals(teacher.getIsActive())) {
                continue;
            }
            User user = userRepository.findById(teacher.getUserId()).orElse(null);
            if (!isActiveEmailUser(user) || user.getRole() != User.Role.TEACHER) {
                continue;
            }
            sendAnnouncementEmail(
                user.getEmail(),
                teacher.getFullName(),
                announcement,
                portalUrl,
                updated);
        }
    }

    private void sendParentAnnouncement(
        User user,
        Parent parent,
        Announcement announcement,
        String portalUrl,
        boolean updated) {
        sendAnnouncementEmail(
            user.getEmail(),
            parent.getFullName(),
            announcement,
            portalUrl,
            updated);
    }

    private void sendAnnouncementEmail(
        String email,
        String name,
        Announcement announcement,
        String portalUrl,
        boolean updated) {
        if (updated) {
            emailService.sendAnnouncementUpdated(
                email,
                name,
                announcement.getTitle(),
                announcement.getContent(),
                portalUrl);
        } else {
            emailService.sendAnnouncementPublished(
                email,
                name,
                announcement.getTitle(),
                announcement.getContent(),
                portalUrl);
        }
    }

    private static boolean isActiveEmailUser(User user) {
        return user != null
            && Boolean.TRUE.equals(user.getIsActive())
            && user.getEmail() != null
            && !user.getEmail().isBlank();
    }

    private String parentAnnouncementsUrl() {
        return portalOrigin() + "/parent/announcements";
    }

    private String teacherAnnouncementsUrl() {
        return portalOrigin() + "/teacher/announcements";
    }

    private String portalOrigin() {
        String origin = frontendProperties.getOrigin();
        if (origin == null || origin.isBlank()) {
            return "http://localhost:3000";
        }
        return origin.replaceAll("/$", "");
    }

    private List<Announcement> parentInbox(Long parentId) {
        if (parentId == null) {
            return List.of();
        }
        List<ParentStudent> links = parentStudentRepository.findByParentId(parentId);
        Set<Long> studentIds = links.stream().map(ParentStudent::getStudentId).collect(Collectors.toSet());
        Set<Long> classIds = new HashSet<>();
        if (!studentIds.isEmpty()) {
            for (Student student : studentRepository.findAllById(studentIds)) {
                if (student.getClassId() != null) {
                    classIds.add(student.getClassId());
                }
            }
        }
        Collection<Long> safeClassIds = classIds.isEmpty() ? List.of(-1L) : classIds;
        Collection<Long> safeStudentIds = studentIds.isEmpty() ? List.of(-1L) : studentIds;
        return announcementRepository.findParentInbox(parentId, safeClassIds, safeStudentIds);
    }

    private void enforceTeacherCreateAudience(
        SecurityUser actor,
        AnnouncementAudience audience,
        Long classId,
        List<Long> recipientIds) {
        if (actor.getRole() != User.Role.TEACHER) {
            return;
        }
        if (audience != AnnouncementAudience.CLASS && audience != AnnouncementAudience.SPECIFIC_PUPILS) {
            throw new ForbiddenException("Teachers can only announce to their own class pupils");
        }
        Set<Long> assigned = authorizationService.getAssignedClassIds(actor);
        if (assigned.isEmpty()) {
            throw new ForbiddenException("Not assigned to any class");
        }
        if (audience == AnnouncementAudience.CLASS) {
            if (classId == null || !assigned.contains(classId)) {
                throw new ForbiddenException("You can only send announcements to your assigned classes");
            }
            return;
        }
        if (recipientIds == null || recipientIds.isEmpty()) {
            throw new ConflictException("recipientIds are required for SPECIFIC_PUPILS");
        }
        for (Long studentId : recipientIds) {
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pupil not found"));
            if (student.getClassId() == null || !assigned.contains(student.getClassId())) {
                throw new ForbiddenException("Pupil is not in your assigned classes");
            }
        }
        if (classId != null && !assigned.contains(classId)) {
            throw new ForbiddenException("You can only send announcements to your assigned classes");
        }
    }

    private void requireOwnerOrAdmin(SecurityUser actor, Announcement announcement) {
        if (actor.getRole() == User.Role.ADMIN) {
            return;
        }
        if (actor.getRole() != User.Role.TEACHER
            || !Objects.equals(actor.getUserId(), announcement.getCreatedBy())) {
            throw new ForbiddenException("You can only manage your own announcements");
        }
    }

    private void validateAudience(AnnouncementAudience audience, Long classId, List<Long> recipientIds) {
        if (audience == AnnouncementAudience.CLASS) {
            if (classId == null) {
                throw new ConflictException("classId is required for CLASS audience");
            }
            if (!schoolClassRepository.existsById(classId)) {
                throw new ResourceNotFoundException("Class not found");
            }
        }
        if ((audience == AnnouncementAudience.SPECIFIC_PARENTS
            || audience == AnnouncementAudience.SPECIFIC_PUPILS)
            && (recipientIds == null || recipientIds.isEmpty())) {
            throw new ConflictException("recipientIds are required for " + audience.name());
        }
    }

    private void applyRecipients(Announcement announcement, AnnouncementAudience audience, List<Long> recipientIds) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            return;
        }
        AnnouncementRecipientType type = switch (audience) {
            case SPECIFIC_PARENTS -> AnnouncementRecipientType.PARENT;
            case SPECIFIC_PUPILS -> AnnouncementRecipientType.PUPIL;
            case TEACHERS -> AnnouncementRecipientType.TEACHER;
            default -> null;
        };
        if (type == null) {
            return;
        }
        for (Long recipientId : recipientIds.stream().filter(Objects::nonNull).distinct().toList()) {
            AnnouncementRecipient recipient = new AnnouncementRecipient();
            recipient.setRecipientType(type);
            recipient.setRecipientId(recipientId);
            announcement.addRecipient(recipient);
        }
    }

    private Announcement find(Long id) {
        return announcementRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
    }

    private AnnouncementResponse toResponse(Announcement announcement, Map<Long, String> classNames) {
        List<AnnouncementRecipientResponse> recipients = announcement.getRecipients() == null
            ? List.of()
            : announcement.getRecipients().stream()
                .map(r -> new AnnouncementRecipientResponse(r.getId(), r.getRecipientType(), r.getRecipientId()))
                .toList();
        String className = announcement.getClassId() == null
            ? null
            : classNames.getOrDefault(announcement.getClassId(), "Class #" + announcement.getClassId());
        return AnnouncementResponse.from(announcement, className, recipients);
    }

    private Map<Long, String> classNameMap() {
        return schoolClassRepository.findAll().stream()
            .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName, (a, b) -> a));
    }
}
