import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// En desarrollo, /api y /auth se redirigen al API Gateway local (docker compose, puerto 9080).
// En Azure se usa VITE_API_URL con la URL pública del Gateway.
const gateway = process.env.GATEWAY_URL ?? 'http://localhost:9080'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': gateway,
      '/auth': gateway,
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
  },
})
