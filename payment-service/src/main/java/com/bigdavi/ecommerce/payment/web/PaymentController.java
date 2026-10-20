package com.bigdavi.ecommerce.payment.web;

import com.bigdavi.ecommerce.payment.application.PaymentService;
import com.bigdavi.ecommerce.payment.domain.Payment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> byOrder(@PathVariable UUID orderId) {
        return ResponseEntity.of(paymentService.findByOrder(orderId).map(PaymentResponse::from));
    }

    public record PaymentResponse(UUID id, UUID orderId, BigDecimal amount, String status, String failureReason,
                                  Instant createdAt) {

        static PaymentResponse from(Payment payment) {
            return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getAmount(),
                    payment.getStatus().name(), payment.getFailureReason(), payment.getCreatedAt());
        }
    }
}
