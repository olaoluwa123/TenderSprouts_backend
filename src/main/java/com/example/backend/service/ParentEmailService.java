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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ParentEmailService {

    private final ParentStudentRepository parentStudentRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public ParentEmailService(
        ParentStudentRepository parentStudentRepository,
        ParentRepository parentRepository,
        StudentRepository studentRepository,
        UserRepository userRepository) {
        this.parentStudentRepository = parentStudentRepository;
        this.parentRepository = parentRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    public int emailParentsForStudents(
        Collection<Long> studentIds,
        BiConsumer<User, Parent> emailSender) {
        Map<Long, Parent> byUserId = resolveParentsForStudents(studentIds);
        int emailed = 0;
        for (Parent parent : byUserId.values()) {
            User user = userRepository.findById(parent.getUserId()).orElse(null);
            if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
                continue;
            }
            emailSender.accept(user, parent);
            emailed++;
        }
        return emailed;
    }

    public int emailParentsForClass(Long classId, BiConsumer<User, Parent> emailSender) {
        Set<Long> studentIds = new HashSet<>();
        studentRepository.findByClassId(classId, Pageable.unpaged())
            .forEach(s -> studentIds.add(s.getId()));
        return emailParentsForStudents(studentIds, emailSender);
    }

    public static String formatStudentName(Student student) {
        String first = student.getFirstName() != null ? student.getFirstName() : "";
        String last = student.getLastName() != null ? student.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isBlank() ? student.getAdmissionNumber() : name;
    }

    public List<String> childNamesForParent(Long parentId) {
        return parentStudentRepository.findByParentId(parentId).stream()
            .map(ParentStudent::getStudentId)
            .map(studentRepository::findById)
            .filter(java.util.Optional::isPresent)
            .map(opt -> formatStudentName(opt.get()))
            .distinct()
            .sorted()
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
