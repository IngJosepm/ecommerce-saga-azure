package com.bigdavi.ecommerce.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndCustomerId(UUID id, String customerId);

    List<Order> findTop20ByCustomerIdOrderByCreatedAtDesc(String customerId);
}
