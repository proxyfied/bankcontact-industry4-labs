import { defineConfig } from '@playwright/test'
export default defineConfig({
  testDir: './e2e-ai', workers: 1, fullyParallel: false, retries: 0,
  reporter: [['list'], ['json', { outputFile: '../evidence/lab5/playwright-results.json' }]],
  use: { baseURL: 'http://localhost:8082', viewport: { width: 1440, height: 1050 }, trace: 'retain-on-failure' }
})
