package com.example.backend.repository;

import com.example.backend.entity.Parent;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParentRepository extends JpaRepository<Parent, Long> {

    Optional<Parent> findByUserId(Long userId);

    @Query("""
        SELECT DISTINCT p FROM Parent p
        JOIN ParentStudent ps ON ps.parentId = p.id
        JOIN Student s ON s.id = ps.studentId
        WHERE s.classId IN :classIds
        """)
    Page<Parent> findByStudentsInClassIds(
        @Param("classIds") Collection<Long> classIds,
        Pageable pageable);
}
