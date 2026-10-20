package com.bigdavi.ecommerce.order.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pedido con una máquina de estados mínima:
 * solo un pedido PENDING_PAYMENT puede confirmarse o cancelarse.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private BigDecimal totalAmount;

    private String failureReason;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @Version
    private long version;

    protected Order() {
    }

    public Order(String customerId, List<OrderItem> items) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.items.addAll(items);
        this.status = OrderStatus.PENDING_PAYMENT;
        this.createdAt = Instant.now();
    }

    /** @return false si el pedido ya no estaba pendiente (transición ignorada). */
    public boolean confirm(BigDecimal amount) {
        if (status != OrderStatus.PENDING_PAYMENT) {
            return false;
        }
        this.status = OrderStatus.CONFIRMED;
        this.totalAmount = amount;
        this.updatedAt = Instant.now();
        return true;
    }

    /** @return false si el pedido ya no estaba pendiente (transición ignorada). */
    public boolean cancel(OrderStatus cancelledStatus, String reason) {
        if (status != OrderStatus.PENDING_PAYMENT) {
            return false;
        }
        this.status = cancelledStatus;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
        return true;
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
