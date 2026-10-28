package com.example.backend.repository;

import com.example.backend.entity.EmailOutbox;
import com.example.backend.entity.enums.EmailOutboxStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {

    @Query("""
        SELECT e FROM EmailOutbox e
        WHERE e.status = :status
          AND (e.nextAttemptAt IS NULL OR e.nextAttemptAt <= :now)
        ORDER BY e.id ASC
        """)
    List<EmailOutbox> findDue(
        @Param("status") EmailOutboxStatus status,
        @Param("now") LocalDateTime now,
        Pageable pageable);
}
