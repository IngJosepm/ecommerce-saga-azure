import { useCallback, useEffect, useState } from 'react'
import { api } from './api.js'
import Login from './components/Login.jsx'
import Catalog from './components/Catalog.jsx'
import Cart from './components/Cart.jsx'
import Orders from './components/Orders.jsx'

const SESSION_KEY = 'tienda-saga-session'

function loadSession() {
  try {
    return JSON.parse(sessionStorage.getItem(SESSION_KEY))
  } catch {
    return null
  }
}

export default function App() {
  const [session, setSession] = useState(loadSession)
  const [products, setProducts] = useState([])
  const [cart, setCart] = useState({})
  const [error, setError] = useState(null)
  const [ordersVersion, setOrdersVersion] = useState(0)

  const refreshProducts = useCallback(() => {
    api.products().then(setProducts).catch((e) => setError(e.message))
  }, [])

  useEffect(refreshProducts, [refreshProducts])

  function handleLogin(newSession) {
    try {
      sessionStorage.setItem(SESSION_KEY, JSON.stringify(newSession))
    } catch {
      // sin almacenamiento: la sesión dura lo que la pestaña
    }
    setSession(newSession)
  }

  const logout = useCallback(() => {
    try {
      sessionStorage.removeItem(SESSION_KEY)
    } catch {
      // ignorar
    }
    setSession(null)
    setCart({})
  }, [])

  function addToCart(productId) {
    setCart((current) => ({ ...current, [productId]: (current[productId] ?? 0) + 1 }))
  }

  function updateQuantity(productId, quantity) {
    setCart((current) => {
      const next = { ...current }
      if (quantity <= 0) delete next[productId]
      else next[productId] = quantity
      return next
    })
  }

  async function checkout() {
    setError(null)
    const items = Object.entries(cart).map(([productId, quantity]) => ({ productId, quantity }))
    try {
      await api.createOrder(session.token, items)
      setCart({})
      setOrdersVersion((v) => v + 1)
    } catch (e) {
      if (e.status === 401) logout()
      setError(e.message)
    }
  }

  return (
    <div className="app">
      <header className="topbar">
        <h1>Tienda Saga</h1>
        {session && (
          <div className="user">
            <span>Hola, <strong>{session.username}</strong></span>
            <button className="link" onClick={logout}>Salir</button>
          </div>
        )}
      </header>

      {error && (
        <div className="alert" role="alert">
          {error}
          <button className="link" onClick={() => setError(null)} aria-label="Cerrar">✕</button>
        </div>
      )}

      {!session ? (
        <main className="center">
          <Login onLogin={handleLogin} />
        </main>
      ) : (
        <main className="layout">
          <section className="panel wide">
            <Catalog products={products} onAdd={addToCart} />
          </section>
          <aside className="side">
            <section className="panel">
              <Cart cart={cart} products={products} onChange={updateQuantity} onCheckout={checkout} />
            </section>
            <section className="panel">
              <Orders
                token={session.token}
                version={ordersVersion}
                onSettled={refreshProducts}
                onUnauthorized={logout}
              />
            </section>
          </aside>
        </main>
      )}
    </div>
  )
}
