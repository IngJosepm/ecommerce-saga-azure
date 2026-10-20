package com.bigdavi.ecommerce.common.events;

public record OrderLine(String productId, int quantity) {
}
