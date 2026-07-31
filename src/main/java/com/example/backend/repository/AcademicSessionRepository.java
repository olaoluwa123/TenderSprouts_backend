package com.example.backend.repository;

import com.example.backend.entity.AcademicSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicSessionRepository extends JpaRepository<AcademicSession, Long> {

    Optional<AcademicSession> findFirstByIsActiveTrue();

    List<AcademicSession> findByIsActiveTrue();

    Optional<AcademicSession> findFirstByStartDateGreaterThanOrderByStartDateAsc(LocalDate startDate);

    Optional<AcademicSession> findFirstByIdGreaterThanOrderByIdAsc(Long id);
}
