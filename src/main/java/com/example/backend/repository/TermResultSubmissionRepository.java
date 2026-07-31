package com.example.backend.repository;

import com.example.backend.entity.TermResultSubmission;
import com.example.backend.entity.enums.TermResultSubmissionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermResultSubmissionRepository extends JpaRepository<TermResultSubmission, Long> {

    Optional<TermResultSubmission> findByClassIdAndTermId(Long classId, Long termId);

    List<TermResultSubmission> findByStatusOrderBySubmittedAtDesc(TermResultSubmissionStatus status);

    List<TermResultSubmission> findBySessionIdAndStatusOrderBySubmittedAtDesc(
        Long sessionId, TermResultSubmissionStatus status);

    List<TermResultSubmission> findBySessionId(Long sessionId);

    List<TermResultSubmission> findBySessionIdAndTermId(Long sessionId, Long termId);
}
