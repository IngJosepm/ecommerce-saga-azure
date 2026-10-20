package com.bigdavi.ecommerce.payment;

import com.bigdavi.ecommerce.common.events.PaymentFailedEvent;
import com.bigdavi.ecommerce.common.events.PaymentProcessedEvent;
import com.bigdavi.ecommerce.common.events.StockReservedEvent;
import com.bigdavi.ecommerce.common.test.SagaEvents;
import com.bigdavi.ecommerce.common.test.SagaTestcontainers;
import com.bigdavi.ecommerce.payment.domain.Payment;
import com.bigdavi.ecommerce.payment.domain.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(SagaTestcontainers.class)
class PaymentSagaIntegrationTest {

    @Autowired
    SagaEvents events;

    @Autowired
    PaymentRepository payments;

    @Test
    void apruebaElPagoYPublicaPaymentProcessed() {
        UUID orderId = UUID.randomUUID();

        events.publish(new StockReservedEvent(orderId, "ana", new BigDecimal("215.30")));

        PaymentProcessedEvent processed = events.await(PaymentProcessedEvent.class, orderId);
        assertThat(processed.amount()).isEqualByComparingTo("215.30");
        assertThat(payments.findByOrderId(orderId)).get()
                .extracting(Payment::getStatus).isEqualTo(Payment.Status.APPROVED);
    }

    @Test
    void rechazaMontosSobreElLimiteYPublicaLaCompensacion() {
        UUID orderId = UUID.randomUUID();

        events.publish(new StockReservedEvent(orderId, "ana", new BigDecimal("1899.00")));

        PaymentFailedEvent failed = events.await(PaymentFailedEvent.class, orderId);
        assertThat(failed.reason()).isEqualTo("CARD_DECLINED_INSUFFICIENT_FUNDS");
        assertThat(payments.findByOrderId(orderId)).get()
                .extracting(Payment::getStatus).isEqualTo(Payment.Status.REJECTED);
    }

    @Test
    void nuncaCobraDosVecesElMismoPedido() {
        UUID orderId = UUID.randomUUID();
        StockReservedEvent event = new StockReservedEvent(orderId, "ana", new BigDecimal("50.00"));

        events.publish(event);
        events.publish(event);
        events.publish(new StockReservedEvent(orderId, "ana", new BigDecimal("50.00"))); // otro eventId, mismo pedido

        assertThat(events.all(PaymentProcessedEvent.class, orderId, Duration.ofSeconds(4))).hasSize(1);
    }
}
