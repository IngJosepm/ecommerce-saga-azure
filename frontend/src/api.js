// Cliente HTTP del API Gateway.
// VITE_API_URL vacío = mismo origen (proxy de Vite en local); en Azure, la URL del Gateway.
const BASE_URL = import.meta.env.VITE_API_URL ?? ''

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

async function request(path, { token, method = 'GET', body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })

  if (!response.ok) {
    let detail = response.statusText
    try {
      const problem = await response.json()
      detail = problem.detail ?? problem.title ?? detail
    } catch {
      // respuesta sin cuerpo JSON
    }
    if (response.status === 429) {
      detail = `Demasiadas peticiones, espera ${response.headers.get('Retry-After') ?? 'unos'} s`
    }
    throw new ApiError(response.status, detail)
  }
  return response.status === 204 ? null : response.json()
}

export const api = {
  login: (username, password) =>
    request('/auth/login', { method: 'POST', body: { username, password } }),
  products: () => request('/api/products'),
  orders: (token) => request('/api/orders', { token }),
  createOrder: (token, items) => request('/api/orders', { token, method: 'POST', body: { items } }),
}
