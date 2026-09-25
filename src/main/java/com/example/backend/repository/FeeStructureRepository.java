package com.example.backend.repository;

import com.example.backend.entity.FeeStructure;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {

    List<FeeStructure> findByClassIdAndTermIdOrderByNameAsc(Long classId, Long termId);

    List<FeeStructure> findByClassIdAndTermIdAndIsActiveTrueOrderByNameAsc(Long classId, Long termId);

    Optional<FeeStructure> findByClassIdAndTermIdAndNameIgnoreCase(Long classId, Long termId, String name);

    List<FeeStructure> findByClassIdAndTermIdAndPublishedAtIsNotNullAndIsActiveTrueOrderByNameAsc(
        Long classId, Long termId);

    List<FeeStructure> findByTermIdOrderByNameAsc(Long termId);
}
