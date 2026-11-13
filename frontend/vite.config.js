import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const backendEnv = loadEnv(mode, '../', 'APP_HOST_PORT')
  const frontendEnv = loadEnv(mode, '../', 'FRONTEND_PORT')
  const kafkaEnv = loadEnv(mode, '../', 'KAFKA_UI_HOST_PORT')
  return {
    plugins: [react(), tailwindcss()],
    server: {
      port: Number(frontendEnv.FRONTEND_PORT || 5173),
      proxy: { '/api': `http://localhost:${backendEnv.APP_HOST_PORT || 8080}` },
    },
    define: {
      __KAFKA_UI_URL__: JSON.stringify(`http://localhost:${kafkaEnv.KAFKA_UI_HOST_PORT || 8085}`),
    },
  }
})
