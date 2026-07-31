package com.example.backend.repository;

import com.example.backend.entity.LoginAttempt;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    @Query("""
        SELECT COUNT(l) FROM LoginAttempt l
        WHERE l.email = :email AND l.success = false AND l.attemptedAt >= :since
        """)
    long countFailedAttemptsSince(@Param("email") String email, @Param("since") LocalDateTime since);

    @Query("""
        SELECT COUNT(l) FROM LoginAttempt l
        WHERE l.ipAddress = :ipAddress AND l.success = false AND l.attemptedAt >= :since
        """)
    long countFailedAttemptsByIpSince(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
}
