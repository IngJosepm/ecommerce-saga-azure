// Textos y estilo de cada estado del pedido (máquina de estados del Order Service).
export const ORDER_STATUS = {
  PENDING_PAYMENT: { label: 'Procesando…', tone: 'pending' },
  CONFIRMED: { label: 'Confirmado', tone: 'ok' },
  CANCELLED_OUT_OF_STOCK: { label: 'Cancelado: sin stock', tone: 'error' },
  CANCELLED_PAYMENT_FAILED: { label: 'Cancelado: pago rechazado', tone: 'error' },
  CANCELLED_EXPIRED: { label: 'Cancelado: reserva expirada', tone: 'error' },
}

export function statusInfo(status) {
  return ORDER_STATUS[status] ?? { label: status, tone: 'pending' }
}

export function isPending(order) {
  return order.status === 'PENDING_PAYMENT'
}

/** Total del carrito a partir de { productId: cantidad } y el catálogo. */
export function cartTotal(cart, products) {
  return Object.entries(cart).reduce((total, [productId, quantity]) => {
    const product = products.find((p) => p.id === productId)
    return product ? total + product.price * quantity : total
  }, 0)
}

export const money = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'USD' })
