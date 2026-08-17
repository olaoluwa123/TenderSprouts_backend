package com.example.backend.repository;

import com.example.backend.entity.Payment;
import com.example.backend.entity.enums.PaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByGatewayReference(String gatewayReference);

    List<Payment> findByInvoiceIdAndStatus(Long invoiceId, PaymentStatus status);
}
