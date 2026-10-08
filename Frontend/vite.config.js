import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The browser only sees port 5173, so there is no CORS:
//   /api  -> Spring Boot REST
//   /ws   -> Spring Boot STOMP WebSocket (ws: true is REQUIRED)
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/ws': { target: 'http://localhost:8080', changeOrigin: true, ws: true },
    },
  },
})