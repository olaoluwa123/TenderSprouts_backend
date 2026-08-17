package com.example.backend.repository;

import com.example.backend.entity.StaffAttendance;
import com.example.backend.entity.enums.AttendanceStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffAttendanceRepository extends JpaRepository<StaffAttendance, Long> {

    List<StaffAttendance> findByDate(LocalDate date);

    List<StaffAttendance> findByTeacherIdAndDateBetween(Long teacherId, LocalDate from, LocalDate to);

    Optional<StaffAttendance> findByTeacherIdAndDate(Long teacherId, LocalDate date);

    List<StaffAttendance> findByDateBetweenAndStatusIn(LocalDate from, LocalDate to, List<AttendanceStatus> statuses);
}
