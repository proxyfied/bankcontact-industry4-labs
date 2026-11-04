import { defineConfig } from '@playwright/test'
const phase = process.env.DEPLOYMENT_PHASE || 'before'
export default defineConfig({
  testDir: './e2e-deployment', workers: 1, fullyParallel: false, retries: 0, timeout: 40000,
  reporter: [['list'], ['json', { outputFile: `../evidence/lab8/playwright-${phase}.json` }]],
  use: { baseURL: 'https://localhost:8443', ignoreHTTPSErrors: true, viewport: { width: 1440, height: 1050 } }
})
