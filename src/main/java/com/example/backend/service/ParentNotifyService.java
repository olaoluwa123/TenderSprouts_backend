package com.example.backend.service;

import com.example.backend.entity.Parent;
import com.example.backend.entity.ParentStudent;
import com.example.backend.entity.Student;
import com.example.backend.entity.User;
import com.example.backend.repository.ParentRepository;
import com.example.backend.repository.ParentStudentRepository;
import com.example.backend.repository.StudentRepository;
import com.example.backend.repository.UserRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ParentNotifyService {

    private static final Logger log = LoggerFactory.getLogger(ParentNotifyService.class);

    private final ParentStudentRepository parentStudentRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public ParentNotifyService(
        ParentStudentRepository parentStudentRepository,
        ParentRepository parentRepository,
        StudentRepository studentRepository,
        UserRepository userRepository) {
        this.parentStudentRepository = parentStudentRepository;
        this.parentRepository = parentRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    public int notifyParentsForStudents(
        Collection<Long> studentIds,
        BiConsumer<User, Parent> notifier) {
        Map<Long, Parent> byUserId = resolveParentsForStudents(studentIds);
        int notified = 0;
        for (Parent parent : byUserId.values()) {
            User user = userRepository.findById(parent.getUserId()).orElse(null);
            if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
                continue;
            }
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                log.warn(
                    "Skipping parent email notify: parentId={} userId={} has no email",
                    parent.getId(),
                    parent.getUserId());
                continue;
            }
            notifier.accept(user, parent);
            notified++;
        }
        return notified;
    }

    public int notifyParentsForClass(Long classId, BiConsumer<User, Parent> notifier) {
        Set<Long> studentIds = new HashSet<>();
        studentRepository.findByClassId(classId, Pageable.unpaged())
            .forEach(s -> studentIds.add(s.getId()));
        return notifyParentsForStudents(studentIds, notifier);
    }

    public static String formatStudentName(Student student) {
        String first = student.getFirstName() != null ? student.getFirstName() : "";
        String last = student.getLastName() != null ? student.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isBlank() ? student.getAdmissionNumber() : name;
    }

    public List<String> childNamesForParent(Long parentId) {
        return childNamesForParent(parentId, null);
    }

    public List<String> childNamesForParent(Long parentId, Collection<Long> restrictToStudentIds) {
        Set<Long> restrict = restrictToStudentIds == null || restrictToStudentIds.isEmpty()
            ? null
            : new HashSet<>(restrictToStudentIds);
        return parentStudentRepository.findByParentId(parentId).stream()
            .map(ParentStudent::getStudentId)
            .filter(studentId -> restrict == null || restrict.contains(studentId))
            .map(studentRepository::findById)
            .filter(java.util.Optional::isPresent)
            .map(opt -> formatStudentName(opt.get()))
            .distinct()
            .sorted()
            .toList();
    }

    public List<Long> linkedStudentIdsForParent(Long parentId, Collection<Long> restrictToStudentIds) {
        Set<Long> restrict = restrictToStudentIds == null || restrictToStudentIds.isEmpty()
            ? null
            : new HashSet<>(restrictToStudentIds);
        return parentStudentRepository.findByParentId(parentId).stream()
            .map(ParentStudent::getStudentId)
            .filter(studentId -> restrict == null || restrict.contains(studentId))
            .distinct()
            .toList();
    }

    private Map<Long, Parent> resolveParentsForStudents(Collection<Long> studentIds) {
        Map<Long, Parent> byUserId = new HashMap<>();
        for (Long studentId : studentIds) {
            if (studentRepository.findById(studentId).isEmpty()) {
                continue;
            }
            for (ParentStudent link : parentStudentRepository.findByStudentId(studentId)) {
                parentRepository.findById(link.getParentId()).ifPresent(parent ->
                    byUserId.putIfAbsent(parent.getUserId(), parent));
            }
        }
        return byUserId;
    }
}
