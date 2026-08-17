package com.example.backend.repository;

import com.example.backend.entity.SchoolCalendarEvent;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SchoolCalendarEventRepository extends JpaRepository<SchoolCalendarEvent, Long> {

    List<SchoolCalendarEvent> findTop6ByStartDateGreaterThanEqualOrderByStartDateAsc(LocalDate from);

    @Query("""
        SELECT e FROM SchoolCalendarEvent e
        WHERE e.startDate <= :to
          AND COALESCE(e.endDate, e.startDate) >= :from
        ORDER BY e.startDate ASC
        """)
    List<SchoolCalendarEvent> findOverlapping(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
