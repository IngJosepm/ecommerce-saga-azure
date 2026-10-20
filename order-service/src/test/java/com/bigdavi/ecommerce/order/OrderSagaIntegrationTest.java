package com.bigdavi.ecommerce.order;

import com.bigdavi.ecommerce.common.events.OrderCreatedEvent;
import com.bigdavi.ecommerce.common.events.OrderLine;
import com.bigdavi.ecommerce.common.events.PaymentFailedEvent;
import com.bigdavi.ecommerce.common.events.PaymentProcessedEvent;
import com.bigdavi.ecommerce.common.events.StockReservationExpiredEvent;
import com.bigdavi.ecommerce.common.events.StockReservationFailedEvent;
import com.bigdavi.ecommerce.common.test.SagaEvents;
import com.bigdavi.ecommerce.common.test.SagaTestcontainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SagaTestcontainers.class)
class OrderSagaIntegrationTest {

    @Autowired
    SagaEvents events;

    @Autowired
    Environment environment;

    RestClient http;

    @BeforeEach
    void setUp() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + environment.getProperty("local.server.port"))
                .defaultStatusHandler(status -> true, (request, response) -> { }) // no lanzar en 4xx
                .build();
    }

    @Test
    void crearPedidoResponde202YPublicaOrderCreatedViaOutbox() {
        ResponseEntity<Map> response = createOrder("ana", List.of(Map.of("productId", "KB-01", "quantity", 2)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).containsEntry("status", "PENDING_PAYMENT");
        UUID orderId = UUID.fromString((String) response.getBody().get("id"));
        assertThat(response.getHeaders().getLocation()).hasToString("/api/orders/" + orderId);

        OrderCreatedEvent event = events.await(OrderCreatedEvent.class, orderId);
        assertThat(event.customerId()).isEqualTo("ana");
        assertThat(event.lines()).containsExactly(new OrderLine("KB-01", 2));
    }

    @Test
    void pagoProcesadoConfirmaElPedidoYUnFalloPosteriorNoLoCancela() {
        UUID orderId = newOrder("ana");

        PaymentProcessedEvent paid = new PaymentProcessedEvent(orderId, UUID.randomUUID(), new BigDecimal("179.80"));
        events.publish(paid);
        events.publish(paid); // duplicado: debe ignorarse sin error

        awaitStatus("ana", orderId, "CONFIRMED");
        assertThat(getOrder("ana", orderId).getBody()).containsEntry("totalAmount", 179.80);

        // máquina de estados: un pedido confirmado ya no puede cancelarse
        events.publish(new PaymentFailedEvent(orderId, "LATE_FAILURE"));
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(4))
                .untilAsserted(() -> assertThat(getOrder("ana", orderId).getBody())
                        .containsEntry("status", "CONFIRMED"));
    }

    @Test
    void cadaFalloDeLaSagaCancelaConSuEstado() {
        UUID paymentFailed = newOrder("ana");
        UUID outOfStock = newOrder("ana");
        UUID expired = newOrder("ana");

        events.publish(new PaymentFailedEvent(paymentFailed, "CARD_DECLINED"));
        events.publish(new StockReservationFailedEvent(outOfStock, "OUT_OF_STOCK:KB-01"));
        events.publish(new StockReservationExpiredEvent(expired));

        awaitStatus("ana", paymentFailed, "CANCELLED_PAYMENT_FAILED");
        awaitStatus("ana", outOfStock, "CANCELLED_OUT_OF_STOCK");
        awaitStatus("ana", expired, "CANCELLED_EXPIRED");
        assertThat(getOrder("ana", paymentFailed).getBody()).containsEntry("failureReason", "CARD_DECLINED");
    }

    @Test
    void unClienteNoPuedeVerPedidosDeOtro() {
        UUID orderId = newOrder("ana");

        assertThat(getOrder("ana", orderId).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getOrder("intruso", orderId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rechazaPedidosInvalidos() {
        assertThat(createOrder("ana", List.of()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(createOrder("ana", List.of(Map.of("productId", "KB-01", "quantity", 0))).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private UUID newOrder(String user) {
        ResponseEntity<Map> response = createOrder(user, List.of(Map.of("productId", "KB-01", "quantity", 1)));
        return UUID.fromString((String) response.getBody().get("id"));
    }

    private ResponseEntity<Map> createOrder(String user, List<Map<String, Object>> items) {
        return http.post().uri("/api/orders")
                .header("X-User-Id", user)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("items", items))
                .retrieve()
                .toEntity(Map.class);
    }

    private ResponseEntity<Map> getOrder(String user, UUID orderId) {
        return http.get().uri("/api/orders/{id}", orderId)
                .header("X-User-Id", user)
                .retrieve()
                .toEntity(Map.class);
    }

    private void awaitStatus(String user, UUID orderId, String status) {
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(getOrder(user, orderId).getBody()).containsEntry("status", status));
    }
}
