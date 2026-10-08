import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Brauzer yalniz 5173 portunu gorur, ona gore CORS yaranmir:
//   /api  -> Spring Boot REST
//   /ws   -> Spring Boot STOMP WebSocket (ws: true MƏCBURİDİR)
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