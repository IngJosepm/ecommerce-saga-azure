package com.bigdavi.ecommerce.order.application;

import com.bigdavi.ecommerce.common.events.OrderCreatedEvent;
import com.bigdavi.ecommerce.common.events.OrderLine;
import com.bigdavi.ecommerce.common.idempotency.IdempotencyGuard;
import com.bigdavi.ecommerce.common.outbox.OutboxWriter;
import com.bigdavi.ecommerce.order.domain.Order;
import com.bigdavi.ecommerce.order.domain.OrderItem;
import com.bigdavi.ecommerce.order.domain.OrderRepository;
import com.bigdavi.ecommerce.order.domain.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final OutboxWriter outbox;
    private final IdempotencyGuard idempotency;

    public OrderService(OrderRepository orders, OutboxWriter outbox, IdempotencyGuard idempotency) {
        this.orders = orders;
        this.outbox = outbox;
        this.idempotency = idempotency;
    }

    /** Paso (1) de la Saga: pedido + evento en una sola transacción local. */
    @Transactional
    public Order create(String customerId, List<OrderLine> lines) {
        List<OrderItem> items = lines.stream()
                .map(line -> new OrderItem(line.productId(), line.quantity()))
                .toList();
        Order order = orders.save(new Order(customerId, items));
        outbox.write(new OrderCreatedEvent(order.getId(), customerId, lines));
        log.info("Pedido {} creado en PENDING_PAYMENT", order.getId());
        return order;
    }

    @Transactional
    public void confirm(UUID eventId, UUID orderId, BigDecimal amount) {
        if (!idempotency.markProcessed(eventId)) {
            return;
        }
        Order order = load(orderId);
        if (order.confirm(amount)) {
            log.info("Pedido {} CONFIRMED", orderId);
        } else {
            // Ej.: la reserva expiró y luego llegó el pago aprobado -> habría que reembolsar.
            log.warn("Pago aprobado para el pedido {} que ya estaba en {}: requiere reembolso",
                    orderId, order.getStatus());
        }
    }

    @Transactional
    public void cancel(UUID eventId, UUID orderId, OrderStatus status, String reason) {
        if (!idempotency.markProcessed(eventId)) {
            return;
        }
        Order order = load(orderId);
        if (order.cancel(status, reason)) {
            log.info("Pedido {} {} ({})", orderId, status, reason);
        }
    }

    @Transactional(readOnly = true)
    public Order find(UUID orderId, String customerId) {
        return orders.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public List<Order> findRecent(String customerId) {
        return orders.findTop20ByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    private Order load(UUID orderId) {
        return orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
