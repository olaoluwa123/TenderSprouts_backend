package com.example.backend.repository;

import com.example.backend.entity.AssessmentTypeLookup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentTypeLookupRepository extends JpaRepository<AssessmentTypeLookup, Long> {

    List<AssessmentTypeLookup> findByIsActiveTrueOrderBySortOrderAscCodeAsc();

    List<AssessmentTypeLookup> findAllByOrderBySortOrderAscCodeAsc();

    Optional<AssessmentTypeLookup> findByCode(String code);

    boolean existsByCode(String code);
}
