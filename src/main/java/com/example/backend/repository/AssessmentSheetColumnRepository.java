package com.example.backend.repository;

import com.example.backend.entity.AssessmentSheetColumn;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentSheetColumnRepository extends JpaRepository<AssessmentSheetColumn, Long> {

    List<AssessmentSheetColumn> findBySheetIdOrderBySortOrderAscIdAsc(Long sheetId);

    void deleteBySheetId(Long sheetId);
}
