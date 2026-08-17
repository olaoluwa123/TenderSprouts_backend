package com.example.backend.repository;

import com.example.backend.entity.TeacherTimetableSlot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherTimetableSlotRepository extends JpaRepository<TeacherTimetableSlot, Long> {

    List<TeacherTimetableSlot> findByTeacherIdOrderByDayOfWeekAscStartTimeAsc(Long teacherId);

    Optional<TeacherTimetableSlot> findByIdAndTeacherId(Long id, Long teacherId);
}
