package com.bigdavi.ecommerce.inventory.application;

import com.bigdavi.ecommerce.inventory.domain.ReservationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Evita que el stock quede bloqueado para siempre si el pago nunca responde. */
@Component
public class ReservationExpiryJob {

    private final ReservationRepository reservations;
    private final InventoryService inventoryService;
    private final Duration ttl;

    public ReservationExpiryJob(ReservationRepository reservations, InventoryService inventoryService,
                                @Value("${inventory.reservation-ttl:15m}") Duration ttl) {
        this.reservations = reservations;
        this.inventoryService = inventoryService;
        this.ttl = ttl;
    }

    @Scheduled(fixedDelayString = "${inventory.expiry-check-interval:60s}")
    public void releaseExpired() {
        for (UUID orderId : reservations.findOrdersWithReservationsOlderThan(Instant.now().minus(ttl))) {
            inventoryService.expire(orderId);
        }
    }
}
