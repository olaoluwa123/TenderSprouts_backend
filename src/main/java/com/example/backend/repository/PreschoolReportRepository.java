package com.example.backend.repository;

import com.example.backend.entity.PreschoolReport;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreschoolReportRepository extends JpaRepository<PreschoolReport, Long> {

    Optional<PreschoolReport> findByClassIdAndTermIdAndMonthNumber(Long classId, Long termId, Integer monthNumber);

    List<PreschoolReport> findByClassIdAndPublishedAtIsNotNull(Long classId);

    List<PreschoolReport> findByPublishedAtIsNotNull();
}
