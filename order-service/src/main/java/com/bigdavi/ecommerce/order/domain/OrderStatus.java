package com.bigdavi.ecommerce.order.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED_OUT_OF_STOCK,
    CANCELLED_PAYMENT_FAILED,
    CANCELLED_EXPIRED
}
