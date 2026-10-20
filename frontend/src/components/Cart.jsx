import { useState } from 'react'
import { cartTotal, money } from '../orderStatus.js'

export default function Cart({ cart, products, onChange, onCheckout }) {
  const [sending, setSending] = useState(false)
  const entries = Object.entries(cart)

  async function checkout() {
    setSending(true)
    try {
      await onCheckout()
    } finally {
      setSending(false)
    }
  }

  return (
    <>
      <h2>Carrito</h2>
      {entries.length === 0 ? (
        <p className="muted">Tu carrito está vacío.</p>
      ) : (
        <>
          <ul className="list">
            {entries.map(([productId, quantity]) => {
              const product = products.find((p) => p.id === productId)
              return (
                <li key={productId} className="row">
                  <span>{product?.name ?? productId}</span>
                  <input
                    type="number"
                    min="0"
                    max="100"
                    value={quantity}
                    aria-label={`Cantidad de ${product?.name ?? productId}`}
                    onChange={(e) => onChange(productId, Number(e.target.value))}
                  />
                </li>
              )
            })}
          </ul>
          <p className="total">
            Total estimado <strong>{money.format(cartTotal(cart, products))}</strong>
          </p>
          <button onClick={checkout} disabled={sending}>
            {sending ? 'Enviando…' : 'Comprar'}
          </button>
        </>
      )}
    </>
  )
}
