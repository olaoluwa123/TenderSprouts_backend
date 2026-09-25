package com.example.backend.repository;

import com.example.backend.entity.AssessmentSheetScoreType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentSheetScoreTypeRepository extends JpaRepository<AssessmentSheetScoreType, Long> {

    List<AssessmentSheetScoreType> findBySheetIdOrderBySortOrderAscIdAsc(Long sheetId);

    Optional<AssessmentSheetScoreType> findBySheetIdAndCode(Long sheetId, String code);

    void deleteBySheetId(Long sheetId);
}
