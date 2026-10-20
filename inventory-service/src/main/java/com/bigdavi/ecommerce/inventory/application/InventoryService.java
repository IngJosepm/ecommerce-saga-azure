package com.bigdavi.ecommerce.inventory.application;

import com.bigdavi.ecommerce.common.events.OrderCreatedEvent;
import com.bigdavi.ecommerce.common.events.OrderLine;
import com.bigdavi.ecommerce.common.events.StockReservationExpiredEvent;
import com.bigdavi.ecommerce.common.events.StockReservationFailedEvent;
import com.bigdavi.ecommerce.common.events.StockReservedEvent;
import com.bigdavi.ecommerce.common.idempotency.IdempotencyGuard;
import com.bigdavi.ecommerce.common.outbox.OutboxWriter;
import com.bigdavi.ecommerce.inventory.domain.Product;
import com.bigdavi.ecommerce.inventory.domain.ProductRepository;
import com.bigdavi.ecommerce.inventory.domain.Reservation;
import com.bigdavi.ecommerce.inventory.domain.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final ProductRepository products;
    private final ReservationRepository reservations;
    private final OutboxWriter outbox;
    private final IdempotencyGuard idempotency;

    public InventoryService(ProductRepository products, ReservationRepository reservations,
                            OutboxWriter outbox, IdempotencyGuard idempotency) {
        this.products = products;
        this.reservations = reservations;
        this.outbox = outbox;
        this.idempotency = idempotency;
    }

    /**
     * Paso (2) de la Saga. Todo o nada: si un producto no tiene stock se lanza una excepción,
     * la transacción hace rollback (incluidas las reservas ya hechas) y el listener publica el fallo.
     */
    @Transactional
    public void reserve(OrderCreatedEvent event) {
        if (!idempotency.markProcessed(event.eventId())) {
            return;
        }
        // TreeMap: agrupa líneas repetidas y ordena por id para evitar deadlocks entre pedidos.
        Map<String, Integer> quantities = new TreeMap<>();
        for (OrderLine line : event.lines()) {
            quantities.merge(line.productId(), line.quantity(), Integer::sum);
        }

        BigDecimal amount = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            if (products.tryReserve(entry.getKey(), entry.getValue()) == 0) {
                throw new InsufficientStockException(entry.getKey());
            }
            Product product = products.findById(entry.getKey()).orElseThrow();
            amount = amount.add(product.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
            reservations.save(new Reservation(event.orderId(), entry.getKey(), entry.getValue()));
        }

        outbox.write(new StockReservedEvent(event.orderId(), event.customerId(), amount));
        log.info("Stock reservado para el pedido {} (total {})", event.orderId(), amount);
    }

    /** Se ejecuta en una transacción nueva, después del rollback de reserve(). */
    @Transactional
    public void rejectReservation(OrderCreatedEvent event, String reason) {
        if (!idempotency.markProcessed(event.eventId())) {
            return;
        }
        outbox.write(new StockReservationFailedEvent(event.orderId(), reason));
        log.info("Reserva rechazada para el pedido {}: {}", event.orderId(), reason);
    }

    /** Paso (3a): el pago se aprobó, la reserva pasa a definitiva. */
    @Transactional
    public void confirm(UUID eventId, UUID orderId) {
        if (!idempotency.markProcessed(eventId)) {
            return;
        }
        reservations.findByOrderIdAndStatus(orderId, Reservation.Status.RESERVED).forEach(Reservation::confirm);
    }

    /** Compensación (3b): el pago falló, se devuelven las unidades al stock. */
    @Transactional
    public void compensate(UUID eventId, UUID orderId) {
        if (!idempotency.markProcessed(eventId)) {
            return;
        }
        int released = releaseReservations(orderId);
        log.info("Compensación del pedido {}: {} reservas devueltas al stock", orderId, released);
    }

    /** La reserva expiró sin respuesta del pago: se libera el stock y se avisa al Order Service. */
    @Transactional
    public void expire(UUID orderId) {
        if (releaseReservations(orderId) > 0) {
            outbox.write(new StockReservationExpiredEvent(orderId));
            log.warn("Reserva del pedido {} expirada y liberada", orderId);
        }
    }

    @Transactional(readOnly = true)
    public List<Product> catalog() {
        return products.findAll();
    }

    private int releaseReservations(UUID orderId) {
        List<Reservation> pending = reservations.findByOrderIdAndStatus(orderId, Reservation.Status.RESERVED);
        pending.sort((a, b) -> a.getProductId().compareTo(b.getProductId()));
        for (Reservation reservation : pending) {
            products.release(reservation.getProductId(), reservation.getQuantity());
            reservation.release();
            reservations.save(reservation);
        }
        return pending.size();
    }
}
