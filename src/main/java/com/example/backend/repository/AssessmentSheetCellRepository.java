package com.example.backend.repository;

import com.example.backend.entity.AssessmentSheetCell;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentSheetCellRepository extends JpaRepository<AssessmentSheetCell, Long> {

    List<AssessmentSheetCell> findBySheetId(Long sheetId);

    List<AssessmentSheetCell> findByStudentId(Long studentId);

    List<AssessmentSheetCell> findBySheetIdAndStudentId(Long sheetId, Long studentId);

    Optional<AssessmentSheetCell> findByColumnIdAndStudentIdAndTypeCode(
        Long columnId,
        Long studentId,
        String typeCode);

    void deleteBySheetId(Long sheetId);

    void deleteByColumnId(Long columnId);

    void deleteBySheetIdAndTypeCode(Long sheetId, String typeCode);
}
