package com.bigdavi.ecommerce.order.web;

import com.bigdavi.ecommerce.common.events.OrderLine;
import com.bigdavi.ecommerce.order.application.OrderService;
import com.bigdavi.ecommerce.order.domain.Order;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * X-User-Id lo inyecta el API Gateway a partir del JWT ya validado.
 * Este servicio no es accesible desde Internet, solo a través del Gateway.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    static final String USER_HEADER = "X-User-Id";

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 202 Accepted: el pedido queda PENDING_PAYMENT y la Saga continúa de forma asíncrona. */
    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestHeader(USER_HEADER) String userId,
                                                @Valid @RequestBody CreateOrderRequest request) {
        List<OrderLine> lines = request.items().stream()
                .map(item -> new OrderLine(item.productId(), item.quantity()))
                .toList();
        Order order = orderService.create(userId, lines);
        return ResponseEntity.accepted()
                .location(URI.create("/api/orders/" + order.getId()))
                .body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@RequestHeader(USER_HEADER) String userId, @PathVariable UUID id) {
        return OrderResponse.from(orderService.find(id, userId));
    }

    @GetMapping
    public List<OrderResponse> list(@RequestHeader(USER_HEADER) String userId) {
        return orderService.findRecent(userId).stream().map(OrderResponse::from).toList();
    }

    public record CreateOrderRequest(@NotEmpty @Size(max = 20) List<@Valid Item> items) {

        public record Item(@NotBlank String productId, @Min(1) @Max(100) int quantity) {
        }
    }

    public record OrderResponse(UUID id, String status, BigDecimal totalAmount, String failureReason,
                                List<CreateOrderRequest.Item> items, Instant createdAt) {

        static OrderResponse from(Order order) {
            return new OrderResponse(order.getId(), order.getStatus().name(), order.getTotalAmount(),
                    order.getFailureReason(),
                    order.getItems().stream()
                            .map(i -> new CreateOrderRequest.Item(i.getProductId(), i.getQuantity()))
                            .toList(),
                    order.getCreatedAt());
        }
    }
}
