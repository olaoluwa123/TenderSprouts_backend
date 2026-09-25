package com.example.backend.repository;

import com.example.backend.entity.BehaviouralReport;
import com.example.backend.entity.enums.BehaviourPeriodType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BehaviouralReportRepository extends JpaRepository<BehaviouralReport, Long> {

    List<BehaviouralReport> findByClassIdAndTermIdAndPeriodTypeAndWeekNumberOrderByStudentIdAsc(
        Long classId,
        Long termId,
        BehaviourPeriodType periodType,
        Integer weekNumber);

    Optional<BehaviouralReport> findByStudentIdAndTermIdAndPeriodTypeAndWeekNumber(
        Long studentId,
        Long termId,
        BehaviourPeriodType periodType,
        Integer weekNumber);

    List<BehaviouralReport> findByStudentIdAndTermIdOrderByPeriodTypeAscWeekNumberAsc(
        Long studentId,
        Long termId);

    List<BehaviouralReport> findByStudentIdAndPublishedAtIsNotNullOrderByTermIdAscPeriodTypeAscWeekNumberAsc(
        Long studentId);
}
