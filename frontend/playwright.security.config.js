import { defineConfig } from '@playwright/test'
export default defineConfig({
  testDir: './e2e-security', workers: 1, fullyParallel: false, retries: 0, timeout: 40000,
  reporter: [['list'], ['json', { outputFile: '../evidence/lab7/playwright-results.json' }]],
  use: { baseURL: 'http://localhost:8084', viewport: { width: 1440, height: 1050 }, trace: 'retain-on-failure' }
})
