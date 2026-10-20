package com.bigdavi.ecommerce.inventory;

import com.bigdavi.ecommerce.common.events.OrderCreatedEvent;
import com.bigdavi.ecommerce.common.events.OrderLine;
import com.bigdavi.ecommerce.common.events.PaymentFailedEvent;
import com.bigdavi.ecommerce.common.events.PaymentProcessedEvent;
import com.bigdavi.ecommerce.common.events.StockReservationExpiredEvent;
import com.bigdavi.ecommerce.common.events.StockReservationFailedEvent;
import com.bigdavi.ecommerce.common.events.StockReservedEvent;
import com.bigdavi.ecommerce.common.test.SagaEvents;
import com.bigdavi.ecommerce.common.test.SagaTestcontainers;
import com.bigdavi.ecommerce.inventory.application.InsufficientStockException;
import com.bigdavi.ecommerce.inventory.application.InventoryService;
import com.bigdavi.ecommerce.inventory.domain.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(SagaTestcontainers.class)
@TestPropertySource(properties = {
        "inventory.reservation-ttl=3s",
        "inventory.expiry-check-interval=1s"
})
class InventorySagaIntegrationTest {

    @Autowired
    SagaEvents events;

    @Autowired
    InventoryService inventoryService;

    @Autowired
    ProductRepository products;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void reservaStockCalculaElTotalYLoConfirmaAlPagar() {
        UUID orderId = UUID.randomUUID();
        String mouse = newProduct("35.50", 10);

        events.publish(new OrderCreatedEvent(orderId, "ana", List.of(new OrderLine(mouse, 2))));

        StockReservedEvent reserved = events.await(StockReservedEvent.class, orderId);
        assertThat(reserved.amount()).isEqualByComparingTo("71.00");
        assertThat(available(mouse)).isEqualTo(8);

        events.publish(new PaymentProcessedEvent(orderId, UUID.randomUUID(), reserved.amount()));
        // la reserva confirmada no se libera aunque pase el TTL
        await().during(Duration.ofSeconds(4)).atMost(Duration.ofSeconds(6))
                .untilAsserted(() -> assertThat(available(mouse)).isEqualTo(8));
    }

    @Test
    void compensaDevolviendoElStockCuandoElPagoFalla() {
        UUID orderId = UUID.randomUUID();
        String keyboard = newProduct("89.90", 10);

        events.publish(new OrderCreatedEvent(orderId, "ana", List.of(new OrderLine(keyboard, 3))));
        events.await(StockReservedEvent.class, orderId);
        assertThat(available(keyboard)).isEqualTo(7);

        events.publish(new PaymentFailedEvent(orderId, "CARD_DECLINED"));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(available(keyboard)).isEqualTo(10));
    }

    @Test
    void publicaStockReservationFailedSiNoHayStockYNoTocaNada() {
        UUID orderId = UUID.randomUUID();
        String keyboard = newProduct("89.90", 10);
        String monitor = newProduct("420.00", 5);

        // el teclado sí tiene stock, pero el pedido es "todo o nada"
        events.publish(new OrderCreatedEvent(orderId, "ana",
                List.of(new OrderLine(keyboard, 1), new OrderLine(monitor, 999))));

        StockReservationFailedEvent failed = events.await(StockReservationFailedEvent.class, orderId);
        assertThat(failed.reason()).isEqualTo("OUT_OF_STOCK:" + monitor);
        assertThat(available(monitor)).isEqualTo(5);
        assertThat(available(keyboard)).isEqualTo(10);
    }

    @Test
    void unEventoDuplicadoSeProcesaUnaSolaVez() {
        UUID orderId = UUID.randomUUID();
        String mouse = newProduct("35.50", 10);
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, "ana", List.of(new OrderLine(mouse, 1)));

        events.publish(event);
        events.publish(event); // mismo eventId: RabbitMQ entregó dos veces

        assertThat(events.all(StockReservedEvent.class, orderId, Duration.ofSeconds(3))).hasSize(1);
        assertThat(available(mouse)).isEqualTo(9);
    }

    @Test
    void laReservaSinRespuestaDelPagoExpiraYLiberaElStock() {
        UUID orderId = UUID.randomUUID();
        String monitor = newProduct("420.00", 5);

        events.publish(new OrderCreatedEvent(orderId, "ana", List.of(new OrderLine(monitor, 1))));
        events.await(StockReservedEvent.class, orderId);

        // nunca llega la respuesta del pago: a los 3 s (TTL del test) se libera
        events.await(StockReservationExpiredEvent.class, orderId);
        assertThat(available(monitor)).isEqualTo(5);
    }

    @Test
    void diezComprasSimultaneasDeLaUltimaUnidadSoloUnaTieneExito() throws Exception {
        String lastUnit = newProduct("59.00", 1);
        int buyers = 10;
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < buyers; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    try {
                        inventoryService.reserve(new OrderCreatedEvent(
                                UUID.randomUUID(), "buyer", List.of(new OrderLine(lastUnit, 1))));
                        return true;
                    } catch (InsufficientStockException e) {
                        return false;
                    }
                }));
            }
            start.countDown(); // todos compiten a la vez
        }

        long successes = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                successes++;
            }
        }
        assertThat(successes).isEqualTo(1);
        assertThat(available(lastUnit)).isZero();
    }

    @Test
    void elTotalUsaLosPreciosDelCatalogo() {
        UUID orderId = UUID.randomUUID();
        String keyboard = newProduct("89.90", 10);
        String mouse = newProduct("35.50", 10);

        events.publish(new OrderCreatedEvent(orderId, "ana",
                List.of(new OrderLine(keyboard, 1), new OrderLine(keyboard, 1), new OrderLine(mouse, 1))));

        // líneas repetidas se agrupan: 2 × 89.90 + 35.50
        assertThat(events.await(StockReservedEvent.class, orderId).amount())
                .isEqualByComparingTo(new BigDecimal("215.30"));
    }

    /** Producto exclusivo del test: el job de expiración de otros tests no altera su stock. */
    private String newProduct(String price, int stock) {
        String id = "T-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("insert into products (id, name, price, available) values (?, ?, ?, ?)",
                id, "Producto de test", new BigDecimal(price), stock);
        return id;
    }

    private int available(String productId) {
        return products.findById(productId).orElseThrow().getAvailable();
    }
}
