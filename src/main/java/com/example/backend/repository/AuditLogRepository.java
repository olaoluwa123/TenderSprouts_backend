package com.example.backend.repository;

import com.example.backend.entity.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop15ByOrderByCreatedAtDesc();
}
