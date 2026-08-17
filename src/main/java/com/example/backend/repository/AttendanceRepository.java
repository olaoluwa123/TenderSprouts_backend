package com.example.backend.repository;

import com.example.backend.entity.Attendance;
import com.example.backend.entity.enums.AttendanceStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    long countByDateAndStatus(LocalDate date, AttendanceStatus status);

    long countByDate(LocalDate date);

    long countByStudentIdAndStatus(Long studentId, AttendanceStatus status);

    long countByStudentId(Long studentId);

    @Query("SELECT MAX(a.date) FROM Attendance a")
    LocalDate findLatestDate();

    List<Attendance> findByDate(LocalDate date);

    List<Attendance> findByClassIdAndDate(Long classId, LocalDate date);

    long countByClassIdAndDate(Long classId, LocalDate date);

    long countByClassIdAndDateAndStatus(Long classId, LocalDate date, AttendanceStatus status);

    Optional<Attendance> findByStudentIdAndDate(Long studentId, LocalDate date);

    List<Attendance> findByStudentIdAndDateBetween(Long studentId, LocalDate from, LocalDate to);

    List<Attendance> findByDateBetweenAndStatusIn(LocalDate from, LocalDate to, List<AttendanceStatus> statuses);

    List<Attendance> findByClassIdAndDateBetweenAndStatusIn(
        Long classId, LocalDate from, LocalDate to, List<AttendanceStatus> statuses);

    List<Attendance> findByClassIdAndDateBetween(Long classId, LocalDate from, LocalDate to);

    List<Attendance> findByDateBetween(LocalDate from, LocalDate to);

    @Query("""
        SELECT a.status, COUNT(a)
        FROM Attendance a
        WHERE a.date = :date
        GROUP BY a.status
        """)
    List<Object[]> countByStatusForDate(@Param("date") LocalDate date);
}
