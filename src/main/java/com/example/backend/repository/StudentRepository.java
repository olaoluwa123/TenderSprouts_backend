package com.example.backend.repository;

import com.example.backend.entity.Student;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByAdmissionNumber(String admissionNumber);

    Optional<Student> findByAdmissionNumber(String admissionNumber);

    Page<Student> findByClassId(Long classId, Pageable pageable);

    @Query("""
        SELECT s FROM Student s
        WHERE (:classId IS NULL OR s.classId = :classId)
        AND (
            :search IS NULL
            OR LOWER(CONCAT(COALESCE(s.firstName, ''), ' ', COALESCE(s.lastName, '')))
                LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        """)
    Page<Student> search(@Param("classId") Long classId, @Param("search") String search, Pageable pageable);

    @Query("""
        SELECT s FROM Student s
        JOIN StudentEnrollment e ON e.studentId = s.id
        WHERE e.sessionId = :sessionId AND e.status = com.example.backend.entity.enums.EnrollmentStatus.ACTIVE
        AND (:classId IS NULL OR e.classId = :classId)
        """)
    Page<Student> findByActiveEnrollment(
        @Param("sessionId") Long sessionId,
        @Param("classId") Long classId,
        Pageable pageable);
}
