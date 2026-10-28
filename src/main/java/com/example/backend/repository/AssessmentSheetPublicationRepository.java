package com.example.backend.repository;

import com.example.backend.entity.AssessmentSheetPublication;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentSheetPublicationRepository extends JpaRepository<AssessmentSheetPublication, Long> {

    List<AssessmentSheetPublication> findBySheetId(Long sheetId);

    List<AssessmentSheetPublication> findByStudentId(Long studentId);

    Optional<AssessmentSheetPublication> findBySheetIdAndStudentId(Long sheetId, Long studentId);
}
