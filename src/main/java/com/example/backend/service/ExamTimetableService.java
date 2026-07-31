package com.example.backend.service;

import com.example.backend.config.AppFrontendProperties;
import com.example.backend.dto.examtimetable.CreateExamTimetableSlotRequest;
import com.example.backend.dto.examtimetable.ExamTimetableSlotResponse;
import com.example.backend.dto.examtimetable.PublishExamTimetableResponse;
import com.example.backend.dto.examtimetable.UpdateExamTimetableSlotRequest;
import com.example.backend.entity.ExamTimetableSlot;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Student;
import com.example.backend.entity.StudentEnrollment;
import com.example.backend.entity.Subject;
import com.example.backend.entity.Teacher;
import com.example.backend.entity.Term;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.EnrollmentStatus;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.ClassSubjectRepository;
import com.example.backend.repository.ExamTimetableSlotRepository;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.StudentEnrollmentRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TermRepository;
import com.example.backend.security.SecurityUser;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamTimetableService {

    private final ExamTimetableSlotRepository examTimetableSlotRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final TermRepository termRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final ActiveSessionService activeSessionService;
    private final ActiveTermService activeTermService;
    private final AuthorizationService authorizationService;
    private final ParentEmailService parentEmailService;
    private final EmailService emailService;
    private final AppFrontendProperties frontendProperties;
    private final AuditService auditService;

    public ExamTimetableService(
        ExamTimetableSlotRepository examTimetableSlotRepository,
        SchoolClassRepository schoolClassRepository,
        SubjectRepository subjectRepository,
        TeacherRepository teacherRepository,
        TermRepository termRepository,
        ClassSubjectRepository classSubjectRepository,
        StudentRepository studentRepository,
        StudentEnrollmentRepository studentEnrollmentRepository,
        ActiveSessionService activeSessionService,
        ActiveTermService activeTermService,
        AuthorizationService authorizationService,
        ParentEmailService parentEmailService,
        EmailService emailService,
        AppFrontendProperties frontendProperties,
        AuditService auditService) {
        this.examTimetableSlotRepository = examTimetableSlotRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.termRepository = termRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.studentRepository = studentRepository;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.activeSessionService = activeSessionService;
        this.activeTermService = activeTermService;
        this.authorizationService = authorizationService;
        this.parentEmailService = parentEmailService;
        this.emailService = emailService;
        this.frontendProperties = frontendProperties;
        this.auditService = auditService;
    }

    public List<ExamTimetableSlotResponse> listForClass(
        Long classId,
        Long termId,
        boolean includeUnpublished,
        SecurityUser actor) {
        schoolClassRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        termRepository.findById(termId)
            .orElseThrow(() -> new ResourceNotFoundException("Term not found"));
        if (actor.getRole() != User.Role.ADMIN) {
            authorizationService.requireTeacherForClass(actor, classId);
        }
        List<ExamTimetableSlot> slots = includeUnpublished || actor.getRole() == User.Role.ADMIN
            ? examTimetableSlotRepository.findByClassIdAndTermIdOrderByExamDateAscStartTimeAsc(classId, termId)
            : examTimetableSlotRepository
                .findByClassIdAndTermIdAndPublishedAtIsNotNullOrderByExamDateAscStartTimeAsc(classId, termId);
        return slots.stream().map(this::toResponse).toList();
    }

    public List<ExamTimetableSlotResponse> listForStudent(
        Long studentId,
        Long termId,
        SecurityUser actor) {
        authorizationService.requireParentOrStaffForStudentRead(actor, studentId);
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        Long classId = resolveClassId(student, termId);
        return examTimetableSlotRepository
            .findByClassIdAndTermIdAndPublishedAtIsNotNullOrderByExamDateAscStartTimeAsc(classId, termId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public ExamTimetableSlotResponse create(
        Long classId,
        CreateExamTimetableSlotRequest request,
        SecurityUser actor) {
        schoolClassRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        termRepository.findById(request.termId())
            .orElseThrow(() -> new ResourceNotFoundException("Term not found"));
        activeTermService.requireMatchesActiveTermForWrite(request.termId(), actor);
        if (actor.getRole() != User.Role.ADMIN) {
            authorizationService.requireTeacherForClass(actor, classId);
        }
        validateSubjectForClass(classId, request.subjectId());
        if (request.teacherId() != null) {
            teacherRepository.findById(request.teacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        }
        if (examTimetableSlotRepository.findByClassIdAndSubjectIdAndTermIdAndExamDateAndStartTime(
            classId, request.subjectId(), request.termId(), request.examDate(), request.startTime()).isPresent()) {
            throw new ConflictException("Exam slot already exists for this subject, date, and time");
        }
        if (!request.endTime().isAfter(request.startTime())) {
            throw new ConflictException("End time must be after start time");
        }

        ExamTimetableSlot slot = new ExamTimetableSlot();
        slot.setClassId(classId);
        slot.setSubjectId(request.subjectId());
        slot.setTeacherId(request.teacherId());
        slot.setTermId(request.termId());
        slot.setSessionId(request.sessionId() != null
            ? request.sessionId()
            : activeSessionService.requireActiveSessionId());
        slot.setExamDate(request.examDate());
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setRoom(request.room());
        slot.setInstructions(request.instructions());
        slot = examTimetableSlotRepository.save(slot);
        auditService.logAdminAction(actor, "CREATE", "exam_timetable_slot", slot.getId(), null);
        return toResponse(slot);
    }

    @Transactional
    public ExamTimetableSlotResponse update(
        Long classId,
        Long slotId,
        UpdateExamTimetableSlotRequest request,
        SecurityUser actor) {
        ExamTimetableSlot slot = findSlot(classId, slotId);
        if (actor.getRole() != User.Role.ADMIN) {
            authorizationService.requireTeacherForClass(actor, classId);
        }
        if (request.subjectId() != null) {
            validateSubjectForClass(classId, request.subjectId());
            slot.setSubjectId(request.subjectId());
        }
        if (request.teacherId() != null) {
            slot.setTeacherId(request.teacherId());
        }
        if (request.examDate() != null) {
            slot.setExamDate(request.examDate());
        }
        if (request.startTime() != null) {
            slot.setStartTime(request.startTime());
        }
        if (request.endTime() != null) {
            slot.setEndTime(request.endTime());
        }
        if (request.room() != null) {
            slot.setRoom(request.room());
        }
        if (request.instructions() != null) {
            slot.setInstructions(request.instructions());
        }
        if (!slot.getEndTime().isAfter(slot.getStartTime())) {
            throw new ConflictException("End time must be after start time");
        }
        slot = examTimetableSlotRepository.save(slot);
        auditService.logAdminAction(actor, "UPDATE", "exam_timetable_slot", slot.getId(), null);
        return toResponse(slot);
    }

    @Transactional
    public void delete(Long classId, Long slotId, SecurityUser actor) {
        ExamTimetableSlot slot = findSlot(classId, slotId);
        if (actor.getRole() != User.Role.ADMIN) {
            authorizationService.requireTeacherForClass(actor, classId);
        }
        examTimetableSlotRepository.delete(slot);
        auditService.logAdminAction(actor, "DELETE", "exam_timetable_slot", slotId, null);
    }

    @Transactional
    public PublishExamTimetableResponse publish(Long classId, Long termId, SecurityUser actor) {
        schoolClassRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        termRepository.findById(termId)
            .orElseThrow(() -> new ResourceNotFoundException("Term not found"));
        activeTermService.requireMatchesActiveTermForWrite(termId, actor);

        List<ExamTimetableSlot> slots = examTimetableSlotRepository
            .findByClassIdAndTermIdOrderByExamDateAscStartTimeAsc(classId, termId);
        if (slots.isEmpty()) {
            throw new ResourceNotFoundException("No exam timetable slots found");
        }

        LocalDateTime now = LocalDateTime.now();
        int published = 0;
        for (ExamTimetableSlot slot : slots) {
            if (slot.getPublishedAt() == null) {
                slot.setPublishedAt(now);
                examTimetableSlotRepository.save(slot);
                published++;
            }
        }

        String className = schoolClassRepository.findById(classId).map(SchoolClass::getName).orElse("class");
        String portalUrl = frontendProperties.getOrigin().replaceAll("/$", "") + "/exam-timetable";
        int parentsNotified = parentEmailService.emailParentsForClass(
            classId,
            (user, parent) -> emailService.sendExamTimetablePublished(
                user.getEmail(),
                parent.getFullName(),
                className,
                portalUrl));

        auditService.logAdminAction(actor, "PUBLISH", "exam_timetable", classId, "termId=" + termId);
        return new PublishExamTimetableResponse(published, parentsNotified);
    }

    private Long resolveClassId(Student student, Long termId) {
        Term term = termRepository.findById(termId).orElseThrow();
        return studentEnrollmentRepository.findByStudentIdAndSessionId(student.getId(), term.getSessionId())
            .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
            .map(StudentEnrollment::getClassId)
            .orElse(student.getClassId());
    }

    private void validateSubjectForClass(Long classId, Long subjectId) {
        subjectRepository.findById(subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        if (classSubjectRepository.findByClassIdAndSubjectId(classId, subjectId).isEmpty()) {
            throw new ForbiddenException("Subject is not assigned to this class");
        }
    }

    private ExamTimetableSlot findSlot(Long classId, Long slotId) {
        ExamTimetableSlot slot = examTimetableSlotRepository.findById(slotId)
            .orElseThrow(() -> new ResourceNotFoundException("Exam timetable slot not found"));
        if (!slot.getClassId().equals(classId)) {
            throw new ResourceNotFoundException("Exam timetable slot not found");
        }
        return slot;
    }

    private ExamTimetableSlotResponse toResponse(ExamTimetableSlot slot) {
        String className = schoolClassRepository.findById(slot.getClassId()).map(SchoolClass::getName).orElse(null);
        String subjectName = subjectRepository.findById(slot.getSubjectId()).map(Subject::getName).orElse(null);
        String teacherName = slot.getTeacherId() != null
            ? teacherRepository.findById(slot.getTeacherId()).map(Teacher::getFullName).orElse(null)
            : null;
        return ExamTimetableSlotResponse.from(slot, className, subjectName, teacherName);
    }
}
