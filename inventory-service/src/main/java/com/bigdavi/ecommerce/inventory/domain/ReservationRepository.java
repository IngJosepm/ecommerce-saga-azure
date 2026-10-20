package com.bigdavi.ecommerce.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByOrderIdAndStatus(UUID orderId, Reservation.Status status);

    @Query("select distinct r.orderId from Reservation r where r.status = 'RESERVED' and r.createdAt < :limit")
    List<UUID> findOrdersWithReservationsOlderThan(@Param("limit") Instant limit);
}
