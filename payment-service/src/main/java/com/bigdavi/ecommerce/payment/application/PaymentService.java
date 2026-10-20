package com.bigdavi.ecommerce.payment.application;

import com.bigdavi.ecommerce.common.events.PaymentFailedEvent;
import com.bigdavi.ecommerce.common.events.PaymentProcessedEvent;
import com.bigdavi.ecommerce.common.events.StockReservedEvent;
import com.bigdavi.ecommerce.common.idempotency.IdempotencyGuard;
import com.bigdavi.ecommerce.common.outbox.OutboxWriter;
import com.bigdavi.ecommerce.payment.domain.Payment;
import com.bigdavi.ecommerce.payment.domain.PaymentRepository;
import com.bigdavi.ecommerce.payment.gateway.GatewayResult;
import com.bigdavi.ecommerce.payment.gateway.ResilientPaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final ResilientPaymentGateway gateway;
    private final OutboxWriter outbox;
    private final IdempotencyGuard idempotency;
    private final TransactionTemplate transaction;

    public PaymentService(PaymentRepository payments, ResilientPaymentGateway gateway, OutboxWriter outbox,
                          IdempotencyGuard idempotency, TransactionTemplate transaction) {
        this.payments = payments;
        this.gateway = gateway;
        this.outbox = outbox;
        this.idempotency = idempotency;
        this.transaction = transaction;
    }

    /**
     * Paso (3) de la Saga. La llamada a la pasarela va FUERA de la transacción
     * (no se retiene una conexión de BD mientras se espera a un sistema externo);
     * el resultado y su evento se guardan después en una transacción corta.
     */
    public void process(StockReservedEvent event) {
        if (idempotency.alreadyProcessed(event.eventId()) || payments.findByOrderId(event.orderId()).isPresent()) {
            return;
        }

        GatewayResult result = gateway.charge(event.orderId(), event.amount());

        transaction.executeWithoutResult(status -> {
            if (!idempotency.markProcessed(event.eventId())) {
                return;
            }
            if (result.approved()) {
                Payment payment = payments.save(
                        Payment.approved(event.orderId(), event.amount(), result.transactionId()));
                outbox.write(new PaymentProcessedEvent(event.orderId(), payment.getId(), event.amount()));
                log.info("Pago APROBADO para el pedido {} ({})", event.orderId(), event.amount());
            } else {
                payments.save(Payment.rejected(event.orderId(), event.amount(), result.failureReason()));
                outbox.write(new PaymentFailedEvent(event.orderId(), result.failureReason()));
                log.info("Pago RECHAZADO para el pedido {}: {}", event.orderId(), result.failureReason());
            }
        });
    }

    public Optional<Payment> findByOrder(UUID orderId) {
        return payments.findByOrderId(orderId);
    }
}
