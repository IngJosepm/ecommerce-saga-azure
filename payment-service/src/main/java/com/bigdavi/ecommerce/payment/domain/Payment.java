package com.bigdavi.ecommerce.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    public enum Status { APPROVED, REJECTED }

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID orderId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private String gatewayTransactionId;

    private String failureReason;

    @Column(nullable = false)
    private Instant createdAt;

    protected Payment() {
    }

    private Payment(UUID orderId, BigDecimal amount, Status status, String gatewayTransactionId, String failureReason) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.gatewayTransactionId = gatewayTransactionId;
        this.failureReason = failureReason;
        this.createdAt = Instant.now();
    }

    public static Payment approved(UUID orderId, BigDecimal amount, String gatewayTransactionId) {
        return new Payment(orderId, amount, Status.APPROVED, gatewayTransactionId, null);
    }

    public static Payment rejected(UUID orderId, BigDecimal amount, String reason) {
        return new Payment(orderId, amount, Status.REJECTED, null, reason);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Status getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
