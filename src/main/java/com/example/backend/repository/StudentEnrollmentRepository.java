package com.example.backend.repository;

import com.example.backend.entity.StudentEnrollment;
import com.example.backend.entity.enums.EnrollmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {

    List<StudentEnrollment> findByStudentIdOrderByStartDateDesc(Long studentId);

    Optional<StudentEnrollment> findByStudentIdAndSessionId(Long studentId, Long sessionId);

    Optional<StudentEnrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status);

    List<StudentEnrollment> findByClassIdAndSessionIdAndStatus(
        Long classId, Long sessionId, EnrollmentStatus status);

    long countBySessionIdAndStatus(Long sessionId, EnrollmentStatus status);

    long countByClassIdAndSessionIdAndStatus(Long classId, Long sessionId, EnrollmentStatus status);
}
