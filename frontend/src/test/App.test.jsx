import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from '../App.jsx'
import { cartTotal, statusInfo } from '../orderStatus.js'

const products = [
  { id: 'KB-01', name: 'Teclado mecánico', price: 89.9, available: 50 },
  { id: 'LAST-UNIT', name: 'Edición limitada', price: 59, available: 0 },
]

function jsonResponse(body, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  }))
}

describe('orderStatus', () => {
  it('traduce los estados de la Saga', () => {
    expect(statusInfo('CONFIRMED')).toEqual({ label: 'Confirmado', tone: 'ok' })
    expect(statusInfo('CANCELLED_PAYMENT_FAILED').tone).toBe('error')
  })

  it('calcula el total del carrito con los precios del catálogo', () => {
    expect(cartTotal({ 'KB-01': 2, 'LAST-UNIT': 1 }, products)).toBeCloseTo(238.8)
  })
})

describe('App', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.stubGlobal('fetch', vi.fn((url, options) => {
      if (url.endsWith('/auth/login')) {
        const { password } = JSON.parse(options.body)
        return password === 'demo123'
          ? jsonResponse({ accessToken: 'token-123' })
          : jsonResponse({ detail: 'Credenciales inválidas' }, 401)
      }
      if (url.endsWith('/api/products')) return jsonResponse(products)
      if (url.endsWith('/api/orders') && !options?.method) return jsonResponse([])
      return jsonResponse({}, 404)
    }))
  })

  afterEach(() => vi.unstubAllGlobals())

  it('muestra error con credenciales incorrectas', async () => {
    render(<App />)
    await userEvent.type(screen.getByLabelText('Contraseña'), 'mala')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))
    expect(await screen.findByText('Usuario o contraseña incorrectos')).toBeInTheDocument()
  })

  it('tras el login muestra el catálogo y deshabilita productos agotados', async () => {
    render(<App />)
    await userEvent.type(screen.getByLabelText('Contraseña'), 'demo123')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByText('Teclado mecánico')).toBeInTheDocument()
    expect(screen.getByText('Agotado')).toBeInTheDocument()
    const buttons = screen.getAllByRole('button', { name: 'Agregar' })
    expect(buttons[0]).toBeEnabled()
    expect(buttons[1]).toBeDisabled()

    // la contraseña nunca se envía a otro endpoint que no sea /auth/login
    const productCall = fetch.mock.calls.find(([url]) => url.endsWith('/api/products'))
    expect(productCall[1].headers.Authorization).toBeUndefined()
  })
})
