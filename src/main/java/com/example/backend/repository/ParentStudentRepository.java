package com.example.backend.repository;

import com.example.backend.entity.ParentStudent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentStudentRepository extends JpaRepository<ParentStudent, Long> {

    List<ParentStudent> findByParentId(Long parentId);

    boolean existsByParentIdAndStudentId(Long parentId, Long studentId);

    List<ParentStudent> findByStudentId(Long studentId);
}
