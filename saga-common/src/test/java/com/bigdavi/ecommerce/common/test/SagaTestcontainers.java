package com.bigdavi.ecommerce.common.test;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * PostgreSQL y RabbitMQ reales en Docker para los tests de integración.
 * Son estáticos: se arrancan una vez y los comparten todos los contextos de Spring del módulo.
 * @ServiceConnection configura datasource y RabbitMQ automáticamente (sin URLs a mano).
 */
@TestConfiguration(proxyBeanMethods = false)
public class SagaTestcontainers {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:4-management-alpine");

    @Bean(destroyMethod = "")
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }

    @Bean(destroyMethod = "")
    @ServiceConnection
    RabbitMQContainer rabbitMQContainer() {
        return RABBITMQ;
    }

    @Bean
    SagaEvents sagaEvents(RabbitTemplate rabbitTemplate) {
        return new SagaEvents(rabbitTemplate);
    }
}
