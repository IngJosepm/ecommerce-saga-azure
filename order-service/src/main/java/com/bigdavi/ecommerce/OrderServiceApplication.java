package com.bigdavi.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Está en el paquete raíz com.bigdavi.ecommerce para que el escaneo de componentes
 * incluya también las clases compartidas de saga-common (outbox, idempotencia, mensajería).
 */
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
