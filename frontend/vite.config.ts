import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development /api is proxied to the Spring Boot backend so cookies stay same-origin.
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: false } } },
});
