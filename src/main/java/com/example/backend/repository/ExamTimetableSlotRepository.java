package com.example.backend.repository;

import com.example.backend.entity.ExamTimetableSlot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamTimetableSlotRepository extends JpaRepository<ExamTimetableSlot, Long> {

    List<ExamTimetableSlot> findByClassIdAndTermIdOrderByExamDateAscStartTimeAsc(Long classId, Long termId);

    List<ExamTimetableSlot> findByClassIdAndTermIdAndPublishedAtIsNotNullOrderByExamDateAscStartTimeAsc(
        Long classId, Long termId);

    Optional<ExamTimetableSlot> findByClassIdAndSubjectIdAndTermIdAndExamDateAndStartTime(
        Long classId, Long subjectId, Long termId, java.time.LocalDate examDate, java.time.LocalTime startTime);
}
