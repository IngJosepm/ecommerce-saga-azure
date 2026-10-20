import { useState } from 'react'
import { api } from '../api.js'

export default function Login({ onLogin }) {
  const [username, setUsername] = useState('demo')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const { accessToken } = await api.login(username, password)
      onLogin({ username, token: accessToken })
    } catch (e) {
      setError(e.status === 401 ? 'Usuario o contraseña incorrectos' : e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <form className="panel login" onSubmit={submit}>
      <h2>Iniciar sesión</h2>
      <label>
        Usuario
        <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
      </label>
      <label>
        Contraseña
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
          required
        />
      </label>
      {error && <p className="error">{error}</p>}
      <button type="submit" disabled={loading}>{loading ? 'Entrando…' : 'Entrar'}</button>
      <p className="hint">Usuarios de demo: <code>demo</code> o <code>ana</code> / <code>demo123</code></p>
    </form>
  )
}
