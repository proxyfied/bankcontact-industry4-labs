import { defineConfig } from '@playwright/test'
export default defineConfig({
  testDir: './e2e-bi', fullyParallel: false, workers: 1, retries: 0, timeout: 30000,
  reporter: [['list'], ['json', { outputFile: '../evidence/lab6/playwright-results.json' }]],
  use: { baseURL: 'http://localhost:8080', viewport: { width: 1440, height: 1100 }, trace: 'retain-on-failure' }
})
