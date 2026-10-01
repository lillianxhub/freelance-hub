import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const { BACKEND_ORIGIN } = loadEnv(mode, '.', '')

  return {
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': `${import.meta.dirname}/src`,
    },
  },
  server: {
    proxy: {
      "/api": {
        target: BACKEND_ORIGIN || "http://localhost:8080",
        changeOrigin: true,
        secure: true,
      },
    },
  },
  }
})
