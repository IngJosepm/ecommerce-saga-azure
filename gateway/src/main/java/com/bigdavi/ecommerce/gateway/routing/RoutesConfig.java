package com.bigdavi.ecommerce.gateway.routing;

import com.bigdavi.ecommerce.gateway.config.GatewayProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.function.Function;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration
public class RoutesConfig {

    static final String USER_HEADER = "X-User-Id";

    @Bean
    RouterFunction<ServerResponse> gatewayRoutes(GatewayProperties properties) {
        GatewayProperties.Services services = properties.services();
        return route("orders")
                .route(path("/api/orders/**").or(path("/api/orders")), http())
                .before(uri(services.orders()))
                .before(authenticatedUserHeader())
                .build()
                .and(route("products")
                        .route(path("/api/products/**").or(path("/api/products")), http())
                        .before(uri(services.inventory()))
                        .before(authenticatedUserHeader())
                        .build())
                .and(route("payments")
                        .route(path("/api/payments/**"), http())
                        .before(uri(services.payments()))
                        .before(authenticatedUserHeader())
                        .build());
    }

    /**
     * Elimina cualquier X-User-Id enviado por el cliente (suplantación) y lo reemplaza
     * por el "sub" del JWT ya validado por Spring Security.
     */
    static Function<ServerRequest, ServerRequest> authenticatedUserHeader() {
        return request -> ServerRequest.from(request)
                .headers(headers -> {
                    headers.remove(USER_HEADER);
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth instanceof JwtAuthenticationToken jwt) {
                        headers.set(USER_HEADER, jwt.getToken().getSubject());
                    }
                })
                .build();
    }
}
