package com.example.backend.repository;

import com.example.backend.entity.TermResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TermResultRepository extends JpaRepository<TermResult, Long>, JpaSpecificationExecutor<TermResult> {

    Optional<TermResult> findByStudentIdAndTermId(Long studentId, Long termId);

    List<TermResult> findByClassIdAndTermIdOrderByAverageScoreDesc(Long classId, Long termId);

    List<TermResult> findByClassIdAndTermIdAndPublishedAtIsNotNull(Long classId, Long termId);

    List<TermResult> findByStudentIdAndPublishedAtIsNotNull(Long studentId);

    List<TermResult> findByTermId(Long termId);

    @Query("""
        SELECT tr.classId, AVG(tr.averageScore)
        FROM TermResult tr
        WHERE tr.termId = :termId AND tr.averageScore IS NOT NULL
        GROUP BY tr.classId
        ORDER BY AVG(tr.averageScore) DESC
        """)
    List<Object[]> averageScoreByClassForTerm(@Param("termId") Long termId);
}
