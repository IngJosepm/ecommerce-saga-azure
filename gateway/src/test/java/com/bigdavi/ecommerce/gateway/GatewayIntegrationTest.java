package com.bigdavi.ecommerce.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gateway completo contra un microservicio falso (HttpServer del JDK) que registra
 * la cabecera X-User-Id que recibe.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "gateway.rate-limit.capacity=1000")
class GatewayIntegrationTest {

    static final AtomicReference<String> receivedUserId = new AtomicReference<>();
    static final HttpServer downstream = startDownstream();

    @DynamicPropertySource
    static void routes(DynamicPropertyRegistry registry) {
        String url = "http://localhost:" + downstream.getAddress().getPort();
        registry.add("gateway.services.orders", () -> url);
        registry.add("gateway.services.inventory", () -> url);
        registry.add("gateway.services.payments", () -> url);
    }

    @AfterAll
    static void stopDownstream() {
        downstream.stop(0);
    }

    @Autowired
    Environment environment;

    RestClient http;

    @BeforeEach
    void setUp() {
        receivedUserId.set(null);
        http = RestClient.builder()
                .baseUrl("http://localhost:" + environment.getProperty("local.server.port"))
                .defaultStatusHandler(status -> true, (request, response) -> { })
                .build();
    }

    @Test
    void loginDevuelveUnJwtYRechazaCredencialesIncorrectas() {
        assertThat(login("demo", "demo123").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login("demo", "demo123").getBody().get("accessToken")).asString().startsWith("eyJ");
        assertThat(login("demo", "mala").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void pedidosSinTokenOConTokenFalsoDevuelven401() {
        assertThat(http.get().uri("/api/orders").retrieve().toBodilessEntity().getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(http.get().uri("/api/orders").header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.e30.x")
                .retrieve().toBodilessEntity().getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(receivedUserId.get()).isNull(); // nunca llegó al microservicio
    }

    @Test
    void reemplazaElXUserIdFalsificadoPorElSujetoDelJwt() {
        String token = (String) login("ana", "demo123").getBody().get("accessToken");

        ResponseEntity<String> response = http.get().uri("/api/orders")
                .header("Authorization", "Bearer " + token)
                .header("X-User-Id", "demo") // intento de suplantación
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(receivedUserId.get()).isEqualTo("ana");
    }

    @Test
    void elCatalogoEsPublicoPeroNoReenviaIdentidad() {
        ResponseEntity<String> response = http.get().uri("/api/products")
                .header("X-User-Id", "demo")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(receivedUserId.get()).isEqualTo("<none>");
    }

    private ResponseEntity<Map> login(String username, String password) {
        return http.post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", username, "password", password))
                .retrieve()
                .toEntity(Map.class);
    }

    private static HttpServer startDownstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                String userId = exchange.getRequestHeaders().getFirst("X-User-Id");
                receivedUserId.set(userId == null ? "<none>" : userId);
                byte[] body = "[]".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
