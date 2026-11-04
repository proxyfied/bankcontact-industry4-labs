import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    allowedHosts: process.env.PREVIEW_HOST ? [process.env.PREVIEW_HOST] : [],
    proxy: { '/api': { target: process.env.API_PROXY_TARGET || 'http://localhost:8080', changeOrigin: true } }
  }
})
