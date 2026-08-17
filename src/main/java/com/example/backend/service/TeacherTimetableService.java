package com.example.backend.service;

import com.example.backend.dto.teacher.CreateTeacherTimetableSlotRequest;
import com.example.backend.dto.teacher.TeacherTimetableSlotResponse;
import com.example.backend.dto.teacher.UpdateTeacherTimetableSlotRequest;
import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.Subject;
import com.example.backend.entity.TeacherTimetableSlot;
import com.example.backend.entity.User;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.SchoolClassRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TeacherClassRepository;
import com.example.backend.repository.TeacherRepository;
import com.example.backend.repository.TeacherTimetableSlotRepository;
import com.example.backend.security.SecurityUser;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherTimetableService {

    private static final Set<DayOfWeek> SCHOOL_DAYS = EnumSet.of(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY);

    private final TeacherTimetableSlotRepository slotRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final AuditService auditService;

    public TeacherTimetableService(
        TeacherTimetableSlotRepository slotRepository,
        TeacherRepository teacherRepository,
        TeacherClassRepository teacherClassRepository,
        SchoolClassRepository schoolClassRepository,
        SubjectRepository subjectRepository,
        AuditService auditService) {
        this.slotRepository = slotRepository;
        this.teacherRepository = teacherRepository;
        this.teacherClassRepository = teacherClassRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<TeacherTimetableSlotResponse> list(Long teacherId, SecurityUser actor) {
        requireSelfOrAdmin(teacherId, actor);
        ensureTeacherExists(teacherId);
        Map<Long, String> classNames = classNameMap();
        Map<Long, String> subjectNames = subjectNameMap();
        return slotRepository.findByTeacherIdOrderByDayOfWeekAscStartTimeAsc(teacherId).stream()
            .map(slot -> toResponse(slot, classNames, subjectNames))
            .toList();
    }

    @Transactional
    public TeacherTimetableSlotResponse create(
        Long teacherId,
        CreateTeacherTimetableSlotRequest request,
        SecurityUser actor) {
        requireSelfOrAdmin(teacherId, actor);
        ensureTeacherExists(teacherId);
        validateSlot(request.dayOfWeek(), request.startTime(), request.endTime(), teacherId, request.classId(), request.subjectId());

        TeacherTimetableSlot slot = new TeacherTimetableSlot();
        slot.setTeacherId(teacherId);
        slot.setDayOfWeek(request.dayOfWeek());
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setClassId(request.classId());
        slot.setSubjectId(request.subjectId());
        slot.setRoom(trimToNull(request.room()));

        TeacherTimetableSlot saved = slotRepository.save(slot);
        auditService.logAdminAction(actor, "CREATE", "teacher_timetable_slot", saved.getId(),
            request.dayOfWeek() + " " + request.startTime());
        return toResponse(saved, classNameMap(), subjectNameMap());
    }

    @Transactional
    public TeacherTimetableSlotResponse update(
        Long teacherId,
        Long slotId,
        UpdateTeacherTimetableSlotRequest request,
        SecurityUser actor) {
        requireSelfOrAdmin(teacherId, actor);
        TeacherTimetableSlot slot = slotRepository.findByIdAndTeacherId(slotId, teacherId)
            .orElseThrow(() -> new ResourceNotFoundException("Timetable slot not found"));

        DayOfWeek day = request.dayOfWeek() != null ? request.dayOfWeek() : slot.getDayOfWeek();
        LocalTime start = request.startTime() != null ? request.startTime() : slot.getStartTime();
        LocalTime end = request.endTime() != null ? request.endTime() : slot.getEndTime();
        Long classId = request.classId() != null ? request.classId() : slot.getClassId();
        Long subjectId = request.subjectId();

        validateSlot(day, start, end, teacherId, classId, subjectId);

        slot.setDayOfWeek(day);
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setClassId(classId);
        slot.setSubjectId(subjectId);
        if (request.room() != null) {
            slot.setRoom(trimToNull(request.room()));
        }

        TeacherTimetableSlot saved = slotRepository.save(slot);
        auditService.logAdminAction(actor, "UPDATE", "teacher_timetable_slot", saved.getId(), null);
        return toResponse(saved, classNameMap(), subjectNameMap());
    }

    @Transactional
    public void delete(Long teacherId, Long slotId, SecurityUser actor) {
        requireSelfOrAdmin(teacherId, actor);
        TeacherTimetableSlot slot = slotRepository.findByIdAndTeacherId(slotId, teacherId)
            .orElseThrow(() -> new ResourceNotFoundException("Timetable slot not found"));
        slotRepository.delete(slot);
        auditService.logAdminAction(actor, "DELETE", "teacher_timetable_slot", slotId, null);
    }

    private void validateSlot(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Long teacherId,
        Long classId,
        Long subjectId) {
        if (dayOfWeek == null || !SCHOOL_DAYS.contains(dayOfWeek)) {
            throw new ConflictException("dayOfWeek must be a weekday (Monday–Friday)");
        }
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new ConflictException("endTime must be after startTime");
        }
        if (!teacherClassRepository.existsByTeacherIdAndClassIdAndIsActiveTrue(teacherId, classId)) {
            throw new ForbiddenException("Not assigned to this class");
        }
        if (!schoolClassRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class not found");
        }
        if (subjectId != null && !subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Subject not found");
        }
    }

    private void requireSelfOrAdmin(Long teacherId, SecurityUser actor) {
        if (actor.getRole() == User.Role.ADMIN) {
            return;
        }
        if (actor.getRole() != User.Role.TEACHER || !Objects.equals(actor.getProfileId(), teacherId)) {
            throw new ForbiddenException("You can only manage your own timetable");
        }
    }

    private void ensureTeacherExists(Long teacherId) {
        if (!teacherRepository.existsById(teacherId)) {
            throw new ResourceNotFoundException("Teacher not found");
        }
    }

    private TeacherTimetableSlotResponse toResponse(
        TeacherTimetableSlot slot,
        Map<Long, String> classNames,
        Map<Long, String> subjectNames) {
        String className = classNames.getOrDefault(slot.getClassId(), "Class #" + slot.getClassId());
        String subjectName = slot.getSubjectId() == null
            ? null
            : subjectNames.getOrDefault(slot.getSubjectId(), "Subject #" + slot.getSubjectId());
        return TeacherTimetableSlotResponse.from(slot, className, subjectName);
    }

    private Map<Long, String> classNameMap() {
        return schoolClassRepository.findAll().stream()
            .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName, (a, b) -> a));
    }

    private Map<Long, String> subjectNameMap() {
        return subjectRepository.findAll().stream()
            .collect(Collectors.toMap(Subject::getId, Subject::getName, (a, b) -> a));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
