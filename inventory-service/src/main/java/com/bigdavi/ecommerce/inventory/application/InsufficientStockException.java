package com.bigdavi.ecommerce.inventory.application;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productId) {
        super("OUT_OF_STOCK:" + productId);
    }
}
