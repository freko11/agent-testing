import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Dashboard login (E1-F3-S2) uses session cookies + CSRF — proxying
    // keeps the browser seeing everything as same-origin in local dev,
    // avoiding CORS/SameSite complications entirely.
    proxy: {
      '/api': 'http://localhost:8080',
      '/actuator': 'http://localhost:8080',
    },
  },
})
