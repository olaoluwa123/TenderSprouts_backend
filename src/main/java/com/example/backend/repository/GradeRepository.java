package com.example.backend.repository;

import com.example.backend.entity.Grade;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findByStudentId(Long studentId);

    List<Grade> findByStudentIdAndTermId(Long studentId, Long termId);

    List<Grade> findByClassIdAndTermIdAndSubjectId(Long classId, Long termId, Long subjectId);

    List<Grade> findByClassIdAndTermId(Long classId, Long termId);

    Optional<Grade> findByStudentIdAndSubjectIdAndTermIdAndAssessmentType(
        Long studentId, Long subjectId, Long termId, String assessmentType);

    List<Grade> findByStudentIdAndSessionIdOrderByUpdatedAtDescCreatedAtDesc(
        Long studentId, Long sessionId, Pageable pageable);

    boolean existsByAssessmentType(String assessmentType);

    @Query("""
        SELECT COUNT(DISTINCT g.studentId)
        FROM Grade g
        WHERE g.classId = :classId AND g.termId = :termId
        """)
    long countDistinctStudentsWithGrades(
        @org.springframework.data.repository.query.Param("classId") Long classId,
        @org.springframework.data.repository.query.Param("termId") Long termId);
}
