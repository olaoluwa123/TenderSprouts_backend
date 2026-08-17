package com.example.backend.repository;

import com.example.backend.entity.Invoice;
import com.example.backend.entity.enums.InvoiceStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByStudentIdAndFeeStructureId(Long studentId, Long feeStructureId);

    List<Invoice> findByStudentIdInOrderByDueDateAscCreatedAtDesc(Collection<Long> studentIds);

    List<Invoice> findByStudentIdOrderByDueDateAscCreatedAtDesc(Long studentId);

    long countByFeeStructureId(Long feeStructureId);

    List<Invoice> findByStudentIdAndStatus(Long studentId, InvoiceStatus status);

    List<Invoice> findByStudentIdInAndStatus(Collection<Long> studentIds, InvoiceStatus status);
}
