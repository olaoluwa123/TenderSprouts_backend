package com.example.backend.repository;

import com.example.backend.entity.ClassSubject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassSubjectRepository extends JpaRepository<ClassSubject, Long> {

    List<ClassSubject> findByClassId(Long classId);

    List<ClassSubject> findByClassIdAndIsActiveTrue(Long classId);

    List<ClassSubject> findBySubjectIdAndIsActiveTrue(Long subjectId);

    Optional<ClassSubject> findByClassIdAndSubjectId(Long classId, Long subjectId);

    long countByClassId(Long classId);

    long countByClassIdAndIsActiveTrue(Long classId);
}
