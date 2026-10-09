import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const { BACKEND_ORIGIN, VITE_API_BASE_URL } = loadEnv(mode, '.', '')

  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        '@': `${import.meta.dirname}/src`,
      },
    },
    server: {
      proxy: {
        [VITE_API_BASE_URL || '/api']: {
          target: BACKEND_ORIGIN || 'http://localhost:8080',
          changeOrigin: true,
          secure: true,
        },
      },
    },
  }
})
