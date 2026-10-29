package com.example.backend.repository;

import com.example.backend.entity.AssessmentSheet;
import com.example.backend.entity.enums.AssessmentSheetType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentSheetRepository extends JpaRepository<AssessmentSheet, Long> {

    Optional<AssessmentSheet> findBySheetTypeAndClassIdAndTermIdAndWeekNumber(
        AssessmentSheetType sheetType,
        Long classId,
        Long termId,
        Integer weekNumber);

    List<AssessmentSheet> findByClassIdAndTermId(Long classId, Long termId);

    List<AssessmentSheet> findByClassIdAndPublishedAtIsNotNull(Long classId);

    List<AssessmentSheet> findBySheetTypeAndTermIdAndWeekNumberAndPublishedAtIsNotNull(
        AssessmentSheetType sheetType,
        Long termId,
        Integer weekNumber);

    List<AssessmentSheet> findBySheetTypeAndSubmittedAtIsNotNullAndPublishedAtIsNullOrderBySubmittedAtDesc(
        AssessmentSheetType sheetType);
}
