package com.example.backend.repository;

import com.example.backend.entity.TeacherSubject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherSubjectRepository extends JpaRepository<TeacherSubject, Long> {

    List<TeacherSubject> findByTeacherId(Long teacherId);

    boolean existsByTeacherIdAndClassIdAndSubjectIdAndIsActiveTrue(
        Long teacherId, Long classId, Long subjectId);

    Optional<TeacherSubject> findByTeacherIdAndClassIdAndSubjectId(
        Long teacherId, Long classId, Long subjectId);
}
