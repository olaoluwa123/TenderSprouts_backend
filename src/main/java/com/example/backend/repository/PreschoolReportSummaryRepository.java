package com.example.backend.repository;

import com.example.backend.entity.PreschoolReportSummary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreschoolReportSummaryRepository extends JpaRepository<PreschoolReportSummary, Long> {

    List<PreschoolReportSummary> findByReportId(Long reportId);

    Optional<PreschoolReportSummary> findByReportIdAndStudentId(Long reportId, Long studentId);
}
