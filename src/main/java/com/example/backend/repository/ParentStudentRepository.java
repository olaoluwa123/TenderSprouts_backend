package com.example.backend.repository;

import com.example.backend.entity.ParentStudent;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParentStudentRepository extends JpaRepository<ParentStudent, Long> {

    List<ParentStudent> findByParentId(Long parentId);

    boolean existsByParentIdAndStudentId(Long parentId, Long studentId);

    boolean existsByStudentId(Long studentId);

    List<ParentStudent> findByStudentId(Long studentId);

    @Query("""
        SELECT DISTINCT ps.studentId
        FROM ParentStudent ps
        WHERE ps.studentId IN :studentIds
        """)
    List<Long> findLinkedStudentIds(@Param("studentIds") Collection<Long> studentIds);
}
