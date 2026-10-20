package com.bigdavi.ecommerce.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * @param services        URLs internas de los microservicios
 * @param jwt             secreto HS256 (mínimo 32 caracteres) y duración del token
 * @param rateLimit       peticiones permitidas por IP en cada ventana
 * @param demoUsers       usuario -> contraseña (solo para la demo; en producción usar Entra ID / Auth0)
 * @param allowedOrigins  orígenes CORS del frontend React
 */
@ConfigurationProperties(prefix = "gateway")
public record GatewayProperties(Services services, Jwt jwt, RateLimit rateLimit,
                                Map<String, String> demoUsers, List<String> allowedOrigins) {

    public record Services(String orders, String inventory, String payments) {
    }

    public record Jwt(String secret, Duration ttl) {
    }

    public record RateLimit(int capacity, Duration period) {
    }
}
