package com.bigdavi.ecommerce.payment.gateway;

/**
 * Respuesta de la pasarela. Un rechazo (tarjeta sin fondos) es una respuesta válida,
 * NO un fallo técnico: no debe abrir el Circuit Breaker.
 */
public record GatewayResult(boolean approved, String transactionId, String failureReason) {

    public static GatewayResult approved(String transactionId) {
        return new GatewayResult(true, transactionId, null);
    }

    public static GatewayResult declined(String reason) {
        return new GatewayResult(false, null, reason);
    }
}
