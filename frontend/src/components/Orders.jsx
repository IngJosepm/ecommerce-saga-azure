import { useEffect, useRef, useState } from 'react'
import { api } from '../api.js'
import { isPending, money, statusInfo } from '../orderStatus.js'

const POLL_INTERVAL_MS = 1500

/**
 * Lista de pedidos. Mientras alguno esté PENDING_PAYMENT, consulta cada 1.5 s:
 * así se ve la Saga avanzar en vivo hasta CONFIRMED o CANCELLED_*.
 */
export default function Orders({ token, version, onSettled, onUnauthorized }) {
  const [orders, setOrders] = useState([])
  const [error, setError] = useState(null)
  const hadPending = useRef(false)

  useEffect(() => {
    let cancelled = false
    let timer

    async function load() {
      try {
        const data = await api.orders(token)
        if (cancelled) return
        setOrders(data)
        setError(null)
        const pending = data.some(isPending)
        if (hadPending.current && !pending) onSettled() // la Saga terminó: refrescar stock
        hadPending.current = pending
        if (pending) timer = setTimeout(load, POLL_INTERVAL_MS)
      } catch (e) {
        if (cancelled) return
        if (e.status === 401) onUnauthorized()
        else setError(e.message)
      }
    }

    load()
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [token, version, onSettled, onUnauthorized])

  return (
    <>
      <h2>Mis pedidos</h2>
      {error && <p className="error">{error}</p>}
      {orders.length === 0 ? (
        <p className="muted">Aún no tienes pedidos.</p>
      ) : (
        <ul className="list">
          {orders.map((order) => {
            const { label, tone } = statusInfo(order.status)
            return (
              <li key={order.id} className="order">
                <div className="row">
                  <code title={order.id}>#{order.id.slice(0, 8)}</code>
                  <span className={`badge ${tone}`}>{label}</span>
                </div>
                <p className="muted small">
                  {order.items.map((i) => `${i.quantity}× ${i.productId}`).join(', ')}
                  {order.totalAmount != null && ` · ${money.format(order.totalAmount)}`}
                </p>
                {order.failureReason && <p className="error small">{order.failureReason}</p>}
              </li>
            )
          })}
        </ul>
      )}
    </>
  )
}
