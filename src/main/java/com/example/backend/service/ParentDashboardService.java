package com.example.backend.service;

import com.example.backend.dto.parent.ChildDashboardSummary;
import com.example.backend.dto.parent.ParentDashboardGradeItem;
import com.example.backend.dto.parent.ParentDashboardResponse;
import com.example.backend.entity.Parent;
import com.example.backend.entity.ParentStudent;
import com.example.backend.entity.Student;
import com.example.backend.entity.User;
import com.example.backend.exception.ForbiddenException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.GradeRepository;
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.TermResultRepository;
import com.example.backend.security.SecurityUser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ParentDashboardService {

    private static final int LATEST_GRADES_LIMIT = 5;

    private final ParentRepository parentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final StudentRepository studentRepository;
    private final GradeRepository gradeRepository;
    private final SubjectRepository subjectRepository;
    private final TermResultRepository termResultRepository;
    private final ActiveSessionService activeSessionService;

    public ParentDashboardService(
        ParentRepository parentRepository,
        ParentStudentRepository parentStudentRepository,
        StudentRepository studentRepository,
        GradeRepository gradeRepository,
        SubjectRepository subjectRepository,
        TermResultRepository termResultRepository,
        ActiveSessionService activeSessionService) {
        this.parentRepository = parentRepository;
        this.parentStudentRepository = parentStudentRepository;
        this.studentRepository = studentRepository;
        this.gradeRepository = gradeRepository;
        this.subjectRepository = subjectRepository;
        this.termResultRepository = termResultRepository;
        this.activeSessionService = activeSessionService;
    }

    public ParentDashboardResponse getDashboard(SecurityUser actor, Long studentIdFilter) {
        if (actor.getRole() != User.Role.PARENT || actor.getProfileId() == null) {
            throw new ForbiddenException("Parent profile required");
        }
        Parent parent = parentRepository.findById(actor.getProfileId())
            .orElseThrow(() -> new ResourceNotFoundException("Parent not found"));
        Long sessionId = activeSessionService.requireActiveSessionId();

        List<Long> childIds = parentStudentRepository.findByParentId(parent.getId()).stream()
            .map(ParentStudent::getStudentId)
            .toList();
        if (childIds.isEmpty()) {
            return new ParentDashboardResponse(sessionId, List.of());
        }
        if (studentIdFilter != null) {
            if (!childIds.contains(studentIdFilter)) {
                throw new ForbiddenException("Not linked to this student");
            }
            childIds = List.of(studentIdFilter);
        }

        List<ChildDashboardSummary> summaries = new ArrayList<>();
        for (Long studentId : childIds) {
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) {
                continue;
            }
            summaries.add(buildChildSummary(student, sessionId));
        }

        return new ParentDashboardResponse(sessionId, summaries);
    }

    private ChildDashboardSummary buildChildSummary(Student student, Long sessionId) {
        List<ParentDashboardGradeItem> latestGrades = loadLatestGrades(student.getId(), sessionId);

        return new ChildDashboardSummary(
            student.getId(),
            formatStudentName(student),
            student.getAdmissionNumber(),
            latestGrades);
    }

    private List<ParentDashboardGradeItem> loadLatestGrades(Long studentId, Long sessionId) {
        Set<Long> approvedTermIds = termResultRepository.findByStudentIdAndPublishedAtIsNotNull(studentId).stream()
            .map(result -> result.getTermId())
            .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        if (approvedTermIds.isEmpty()) {
            return List.of();
        }
        return gradeRepository
            .findByStudentIdAndSessionIdOrderByUpdatedAtDescCreatedAtDesc(
                studentId, sessionId, PageRequest.of(0, LATEST_GRADES_LIMIT))
            .stream()
            .filter(grade -> approvedTermIds.contains(grade.getTermId()))
            .map(grade -> ParentDashboardGradeItem.from(
                grade,
                subjectRepository.findById(grade.getSubjectId()).orElse(null)))
            .toList();
    }

    private static String formatStudentName(Student student) {
        String first = student.getFirstName() != null ? student.getFirstName() : "";
        String last = student.getLastName() != null ? student.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isBlank() ? student.getAdmissionNumber() : name;
    }
}
