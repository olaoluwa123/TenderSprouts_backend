package com.example.backend.repository;

import com.example.backend.entity.Invoice;
import com.example.backend.entity.enums.InvoiceStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByStudentIdAndFeeStructureId(Long studentId, Long feeStructureId);

    List<Invoice> findByStudentIdInOrderByDueDateAscCreatedAtDesc(Collection<Long> studentIds);

    List<Invoice> findByStudentIdOrderByDueDateAscCreatedAtDesc(Long studentId);

    long countByFeeStructureId(Long feeStructureId);

    List<Invoice> findByStudentIdAndStatus(Long studentId, InvoiceStatus status);

    List<Invoice> findByStudentIdInAndStatus(Collection<Long> studentIds, InvoiceStatus status);

    List<Invoice> findByFeeStructureIdInOrderByDueDateAscCreatedAtDesc(Collection<Long> feeStructureIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i WHERE i.id IN :ids ORDER BY i.id")
    List<Invoice> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);
}
