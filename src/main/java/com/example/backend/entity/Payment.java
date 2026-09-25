package com.example.backend.entity;

import com.example.backend.entity.base.AuditableEntity;
import com.example.backend.entity.enums.PaymentStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payments")
public class Payment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @ElementCollection
    @CollectionTable(name = "payment_invoices", joinColumns = @JoinColumn(name = "payment_id"))
    @Column(name = "invoice_id")
    private List<Long> invoiceIds = new ArrayList<>();

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "gateway_reference", length = 255)
    private String gatewayReference;

    @Column(name = "gateway_access_code", length = 255)
    private String gatewayAccessCode;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    public Payment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public List<Long> getInvoiceIds() {
        return invoiceIds;
    }

    public void setInvoiceIds(List<Long> invoiceIds) {
        this.invoiceIds = invoiceIds == null ? new ArrayList<>() : new ArrayList<>(invoiceIds);
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public void setGatewayReference(String gatewayReference) {
        this.gatewayReference = gatewayReference;
    }

    public String getGatewayAccessCode() {
        return gatewayAccessCode;
    }

    public void setGatewayAccessCode(String gatewayAccessCode) {
        this.gatewayAccessCode = gatewayAccessCode;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
