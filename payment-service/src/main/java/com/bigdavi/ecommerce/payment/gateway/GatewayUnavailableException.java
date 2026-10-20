package com.bigdavi.ecommerce.payment.gateway;

public class GatewayUnavailableException extends RuntimeException {

    public GatewayUnavailableException(String message) {
        super(message);
    }
}
