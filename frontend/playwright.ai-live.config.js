import { defineConfig } from '@playwright/test'
export default defineConfig({
  testDir: './e2e-ai-live', workers: 1, retries: 0, timeout: 90000,
  reporter: [['list'], ['json', { outputFile: '../evidence/lab5-live/playwright-results.json' }]],
  use: { baseURL: 'http://localhost:8082', viewport: { width: 1440, height: 1050 }, trace: 'off' }
})
