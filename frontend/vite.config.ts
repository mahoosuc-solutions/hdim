import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    // Polyfill for sockjs-client which expects Node.js global object
    global: 'globalThis',
  },
  server: {
    port: 3000,
    host: true, // Listen on all addresses
    proxy: {
      // Proxy API requests to avoid CORS issues
      '/cql-engine': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
        ws: true, // Enable WebSocket proxying
      },
      '/quality-measure': {
        target: 'http://localhost:8087',
        changeOrigin: true,
        secure: false,
      },
      '/api/sales': {
        target: 'http://localhost:8106',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => `/sales-automation${path}`,
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  }
})
