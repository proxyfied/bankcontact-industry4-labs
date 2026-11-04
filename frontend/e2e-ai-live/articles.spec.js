import { test, expect } from '@playwright/test'
import { mkdir, readFile, writeFile } from 'node:fs/promises'
const output = '../evidence/lab5-live'
test('One real GPT-4o-mini generation through Spring AI and Thymeleaf', async ({ page }) => {
  await mkdir(output + '/screenshots', { recursive: true })
  const citation = await readFile('../ai-articles/src/test/resources/fixtures/citation.txt', 'utf8')
  await page.goto('/articles')
  await expect(page.locator('.test-mode')).toHaveCount(0)
  await expect(page.locator('.warning')).toHaveCount(0)
  await page.getByLabel('Текст бібліографічного опису').fill(citation)
  await page.screenshot({ path: `${output}/screenshots/01-live-input.png`, fullPage: true })
  const responsePromise = page.waitForResponse(r => r.url().endsWith('/generate-all') && r.request().method() === 'POST', { timeout: 65000 })
  await page.getByRole('button', { name: 'Структурувати статтю' }).click()
  const response = await responsePromise
  const result = await response.json()
  await writeFile(`${output}/generation-response.json`, JSON.stringify({
    recordedAt: new Date().toISOString(), source: 'Spring AI -> https://api.openai.com',
    httpStatus: response.status(), response: result
  }, null, 2))
  if (response.ok()) await expect(page.locator('#article-title')).not.toHaveText('')
  else await expect(page.getByRole('alert')).toBeVisible()
  await page.screenshot({ path: `${output}/screenshots/02-live-result.png`, fullPage: true })
  expect(response.status(), 'Live generation must return HTTP 200').toBe(200)
  expect(result.testMode).toBe(false)
  expect(result.model).toBe('gpt-4o-mini')
  expect(result.clock).toBeGreaterThan(0)
  expect(Object.keys(result.article)).toHaveLength(36)
  expect(Object.values(result.article).every(v => typeof v === 'string')).toBe(true)
  expect(result.article.title_of_the_article).toContain('Розвиток штучного інтелекту в економіці')
  expect(result.article.total_volume_of_printed_sheets).toBe('11')
  expect(result.article.language).toBe('ukr')
  expect(result.article.published_in_ukraine).toBe('1')
  expect(result.article.published_abroad).toBe('0')
  expect(result.article.specialized).toBe('Category A')
  expect(result.article.source_text).toBe(citation)
  expect(result.article.citation_text).toBe(citation)
})
