package com.example.backend.repository;

import com.example.backend.entity.SchoolClass;
import com.example.backend.entity.enums.ClassGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    Optional<SchoolClass> findFirstByNameIgnoreCase(String name);

    Page<SchoolClass> findByClassGroup(ClassGroup classGroup, Pageable pageable);

    Page<SchoolClass> findByIdIn(java.util.Collection<Long> ids, Pageable pageable);

    Page<SchoolClass> findByIdInAndClassGroup(
        java.util.Collection<Long> ids, ClassGroup classGroup, Pageable pageable);

    List<SchoolClass> findByClassGroup(ClassGroup classGroup);
}
