package com.example.backend.repository;

import com.example.backend.entity.TeacherClass;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherClassRepository extends JpaRepository<TeacherClass, Long> {

    List<TeacherClass> findByTeacherId(Long teacherId);

    boolean existsByTeacherIdAndClassIdAndIsActiveTrue(Long teacherId, Long classId);

    Optional<TeacherClass> findByTeacherIdAndClassId(Long teacherId, Long classId);

    List<TeacherClass> findByTeacherIdAndIsActiveTrue(Long teacherId);

    List<TeacherClass> findByClassIdAndIsActiveTrue(Long classId);

    long countByIsActiveTrue();
}
