import { test, expect } from '@playwright/test'
import { mkdir } from 'node:fs/promises'
const screenshots = '../evidence/lab4/screenshots'
async function shot(page, name) {
  await mkdir(screenshots, { recursive: true })
  await page.screenshot({ path: `${screenshots}/${name}.png`, fullPage: true })
}
const unique = prefix => `${prefix}-${Date.now()}-${Math.floor(Math.random()*10000)}`
async function apiData(request, path) {
  const r = await request.get(path)
  expect(r.ok()).toBeTruthy()
  return r.json()
}

test('Three pages show real data and use SPA navigation', async ({ page, request }) => {
  const baseline = await apiData(request, '/api/customers?size=5')
  await page.goto('/')
  await expect(page.getByText('Дані синхронізовано', { exact: true })).toBeVisible()
  await expect(page.getByRole('row').filter({ hasText: baseline.content[0].externalCode })).toBeVisible()
  await shot(page, '01-home')
  await page.evaluate(() => { window.__spaNavigation = 'retained' })
  await page.getByRole('navigation').getByRole('link', { name: 'Клієнти', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Клієнтська база.' })).toBeVisible()
  await expect(page.getByRole('row').filter({ hasText: baseline.content[0].externalCode })).toBeVisible()
  expect(await page.evaluate(() => window.__spaNavigation)).toBe('retained')
  await shot(page, '02-customers')
  await page.getByRole('navigation').getByRole('link', { name: 'Про проєкт', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Технології, що об’єднують.' })).toBeVisible()
  expect(await page.evaluate(() => window.__spaNavigation)).toBe('retained')
  await shot(page, '03-about')
})

test('Create, reload, update, cancel deletion and delete a customer', async ({ page, request }) => {
  const code = unique('UI'), name = 'Олена Коваленко', editedName = 'Олена Коваленко — оновлено'
  let id
  try {
    await page.goto('/#/customers')
    await page.getByRole('button', { name: '＋ Додати клієнта', exact: true }).click()
    await page.getByLabel('Код клієнта').fill(code)
    await page.getByLabel('Ім’я клієнта').fill(name)
    await shot(page, '04-create-form')
    await page.getByRole('dialog').getByRole('button', { name: 'Додати клієнта', exact: true }).click()
    await expect(page.getByRole('status')).toContainText(`Клієнта ${name} успішно додано.`)
    const list = await apiData(request, '/api/customers?size=100')
    id = list.content.find(c => c.externalCode === code).id
    await expect(page.getByRole('row').filter({ hasText: code })).toBeVisible()
    await shot(page, '05-created')
    await page.reload()
    const row = page.getByRole('row').filter({ hasText: code })
    await expect(row).toBeVisible()
    await row.getByRole('button', { name: `Редагувати ${name}`, exact: true }).click()
    await page.getByLabel('Ім’я клієнта').fill(editedName)
    await shot(page, '06-edit-form')
    await page.getByRole('button', { name: 'Зберегти зміни', exact: true }).click()
    await expect(page.getByRole('status')).toContainText(`Зміни збережено: ${editedName}.`)
    expect((await apiData(request, `/api/customers/${id}`)).displayName).toBe(editedName)
    await shot(page, '07-updated')
    await row.getByRole('button', { name: `Видалити ${editedName}`, exact: true }).click()
    await page.getByRole('button', { name: 'Скасувати', exact: true }).click()
    expect((await request.get(`/api/customers/${id}`)).status()).toBe(200)
    await row.getByRole('button', { name: `Видалити ${editedName}`, exact: true }).click()
    await shot(page, '08-delete-confirmation')
    await page.getByRole('button', { name: 'Підтвердити видалення', exact: true }).click()
    await expect(page.getByRole('status')).toContainText(`Клієнта ${editedName} видалено.`)
    await expect(row).toHaveCount(0)
    expect((await request.get(`/api/customers/${id}`)).status()).toBe(404)
    await shot(page, '09-deleted')
  } finally { if (id) await request.delete(`/api/customers/${id}`) }
})

test('Duplicate code and linked history display HTTP 409 safely', async ({ page, request }) => {
  const existing = (await apiData(request, '/api/customers?size=100')).content[0]
  await page.goto('/#/customers')
  await page.getByRole('button', { name: '＋ Додати клієнта', exact: true }).click()
  await page.getByLabel('Код клієнта').fill(existing.externalCode)
  await page.getByLabel('Ім’я клієнта').fill('Повторний запис')
  await page.getByRole('dialog').getByRole('button', { name: 'Додати клієнта', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('Конфлікт даних')
  await shot(page, '10-duplicate-error')
  await page.getByRole('button', { name: 'Скасувати', exact: true }).click()
  await page.getByRole('row').filter({ hasText: existing.externalCode }).getByRole('button', { name: `Видалити ${existing.displayName}`, exact: true }).click()
  await page.getByRole('button', { name: 'Підтвердити видалення', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('Конфлікт даних')
  expect((await request.get(`/api/customers/${existing.id}`)).status()).toBe(200)
  await page.getByRole('button', { name: 'Скасувати', exact: true }).click()
})

test('Network failure is caught and retry restores the list', async ({ page }) => {
  await page.route('**/api/customers?*', route => route.abort('failed'))
  await page.goto('/#/customers')
  await expect(page.getByRole('alert')).toContainText('Немає зв’язку із сервером')
  await shot(page, '11-network-error')
  await page.unroute('**/api/customers?*')
  await page.getByRole('button', { name: 'Спробувати ще раз', exact: true }).click()
  await expect(page.getByRole('alert')).toHaveCount(0)
  await expect(page.getByRole('button', { name: '↻ Оновити', exact: true })).toBeEnabled()
  await expect(page.getByRole('row')).not.toHaveCount(1)
})

test('Mobile layout keeps page and forms inside viewport', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/')
  await expect(page.getByText('Дані синхронізовано', { exact: true })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await shot(page, '12-mobile-home')
  await page.getByRole('navigation').getByRole('link', { name: 'Клієнти', exact: true }).click()
  await expect(page.getByRole('button', { name: '↻ Оновити', exact: true })).toBeEnabled()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.getByRole('button', { name: '＋ Додати клієнта', exact: true }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await shot(page, '13-mobile-form')
  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog')).toHaveCount(0)
})

test('Pagination and creating a customer beyond the first page', async ({ page, request }) => {
  const ids = []
  try {
    for (let i = 0; i < 11; i++) {
      const response = await request.post('/api/customers', { data: { externalCode: unique('PAGE'), displayName: `Клієнт пагінації ${i}` } })
      expect(response.status()).toBe(201); ids.push((await response.json()).id)
    }
    await page.goto('/#/customers')
    await expect(page.getByText('Сторінка 1 з 2', { exact: true })).toBeVisible()
    await page.getByRole('button', { name: 'Далі →', exact: true }).click()
    await expect(page.getByText('Сторінка 2 з 2', { exact: true })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Далі →', exact: true })).toBeDisabled()
    await page.getByRole('button', { name: '← Назад', exact: true }).click()
    await expect(page.getByText('Сторінка 1 з 2', { exact: true })).toBeVisible()
    await expect(page.getByRole('button', { name: '← Назад', exact: true })).toBeDisabled()
    const code = unique('LAST')
    await page.getByRole('button', { name: '＋ Додати клієнта', exact: true }).click()
    await page.getByLabel('Код клієнта').fill(code)
    await page.getByLabel('Ім’я клієнта').fill('Новий клієнт на останній сторінці')
    await page.getByRole('dialog').getByRole('button', { name: 'Додати клієнта', exact: true }).click()
    await expect(page.getByRole('status')).toContainText('успішно додано')
    const all = await apiData(request, '/api/customers?size=100')
    const created = all.content.find(c => c.externalCode === code)
    if (created) ids.push(created.id)
    await expect(page.getByRole('status')).toContainText('успішно додано')
    await expect(page.getByText('Сторінка 2 з 2', { exact: true })).toBeVisible()
    await expect(page.getByRole('row').filter({ hasText: code })).toBeVisible()
  } finally { for (const id of ids) await request.delete(`/api/customers/${id}`) }
})

test('Required form fields prevent empty submissions', async ({ page }) => {
  let writes = 0
  page.on('request', r => { if (r.method() === 'POST' && r.url().includes('/api/customers')) writes++ })
  await page.goto('/#/customers?create=1')
  await expect(page.getByRole('dialog')).toBeVisible()
  await page.getByRole('dialog').getByRole('button', { name: 'Додати клієнта', exact: true }).click()
  expect(await page.getByLabel('Код клієнта').evaluate(el => el.validity.valueMissing)).toBe(true)
  await page.getByLabel('Код клієнта').fill('   ')
  await page.getByLabel('Ім’я клієнта').fill('   ')
  await page.getByRole('dialog').getByRole('button', { name: 'Додати клієнта', exact: true }).click()
  expect(await page.getByLabel('Код клієнта').evaluate(el => el.validity.patternMismatch)).toBe(true)
  expect(writes).toBe(0)
})
