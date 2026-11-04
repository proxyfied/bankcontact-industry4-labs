import { test, expect } from '@playwright/test'
import fs from 'node:fs/promises'
import path from 'node:path'
const base = 'https://localhost:8443', out = path.resolve('../evidence/lab8'), shots = path.join(out, 'screenshots')
const password = process.env.DEPLOYMENT_TEST_PASSWORD
if (!password) throw new Error('Set DEPLOYMENT_TEST_PASSWORD to an ephemeral test password')
const phase = process.env.DEPLOYMENT_PHASE || 'before'
async function navigateAfterNetworkChange(page, url) {
  for (let attempt = 0; attempt < 3; attempt++) {
    try { return await page.goto(url) }
    catch (error) {
      if (!String(error).includes('ERR_NETWORK_CHANGED') || attempt === 2) throw error
      await new Promise(resolve => setTimeout(resolve, 500))
    }
  }
}
async function login(page) {
  await page.goto(`${base}/login`)
  await page.getByLabel('Логін', { exact: true }).fill('deployment.user')
  await page.getByLabel('Пароль', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Увійти', exact: true }).click()
  await expect(page).toHaveURL(`${base}/user`)
}
test.beforeAll(async () => { await fs.mkdir(shots, { recursive: true }) })
if (phase === 'before') {
  test('HTTPS proxy, readiness, correct public redirect and Secure JSESSIONID', async ({ page, request }) => {
    const health = await request.get('/actuator/health/readiness')
    expect(health.status()).toBe(200); expect((await health.json()).status).toBe('UP')
    const redirect = await request.get('http://localhost:8088/login', { maxRedirects: 0 })
    expect(redirect.status()).toBe(308); expect(redirect.headers().location).toBe(`${base}/login`)
    const unauth = await request.get('/user', { maxRedirects: 0, headers: { 'X-Forwarded-Proto': 'http', 'X-Forwarded-Host': 'spoofed.invalid', 'X-Forwarded-Port': '81' } })
    expect(unauth.status()).toBe(302); expect(unauth.headers().location).toBe(`${base}/login`)
    await page.goto('/registration')
    await page.getByLabel('Логін', { exact: true }).fill('deployment.user')
    await page.getByLabel('Пароль', { exact: true }).fill(password)
    await page.getByLabel('Повторіть пароль').fill(password)
    await page.getByRole('button', { name: 'Створити акаунт', exact: true }).click()
    await expect(page).toHaveURL(/login\?registered$/)
    await login(page)
    const cookie = (await page.context().cookies()).find(c => c.name === 'JSESSIONID')
    expect(cookie).toBeTruthy(); expect(cookie.secure).toBe(true); expect(cookie.httpOnly).toBe(true); expect(cookie.sameSite).toBe('Lax')
    await fs.writeFile(path.join(out, 'https-cookie-check.json'), JSON.stringify({ httpsStatus: 200, health: 'UP', httpRedirect: 308, publicLoginRedirect: unauth.headers().location, forwardedHeaderSpoofIgnored: true, cookieName: cookie.name, secure: cookie.secure, httpOnly: cookie.httpOnly, sameSite: cookie.sameSite, certificate: 'self-signed CI certificate' }, null, 2))
    await page.screenshot({ path: path.join(shots, '01-https-user.png'), fullPage: true })
    await page.getByRole('button', { name: 'Вийти', exact: true }).click()
  })
  test('RBAC and CSRF remain enforced through Nginx', async ({ page }) => {
    await login(page)
    expect((await page.goto('/admin')).status()).toBe(403)
    await page.screenshot({ path: path.join(shots, '02-https-forbidden.png'), fullPage: true })
    expect((await page.request.post('/logout')).status()).toBe(403)
    expect((await page.request.get('/actuator/env')).status()).toBe(403)
    await page.getByRole('button', { name: 'Вийти', exact: true }).click()
  })
  test('one active session through HTTPS and save old session privately for restart test', async ({ browser }) => {
    const a = await browser.newContext({ ignoreHTTPSErrors: true }), b = await browser.newContext({ ignoreHTTPSErrors: true })
    const first = await a.newPage(), second = await b.newPage()
    await login(first); await login(second)
    await first.goto(`${base}/user`); await expect(first).toHaveURL(/login\?expired$/)
    await first.screenshot({ path: path.join(shots, '03-https-expired-session.png'), fullPage: true })
    expect((await second.goto(`${base}/user`)).status()).toBe(200)
    const privateState = path.resolve('../deploy/runtime/test-session.json')
    await fs.writeFile(privateState, JSON.stringify(await b.storageState()), { mode: 0o600 })
    await fs.writeFile(path.join(out, 'session-check.json'), JSON.stringify({ isolatedBrowserContexts: 2, firstRedirect: '/login?expired', secondProtectedStatus: 200, cookieValuesExported: false }, null, 2))
    await a.close(); await b.close()
  })
} else {
  test('database survived app recreation and database restart, old in-memory session expired', async ({ page, browser }) => {
    const old = await browser.newContext({ ignoreHTTPSErrors: true, storageState: path.resolve('../deploy/runtime/test-session.json') })
    const previous = await old.newPage(); await navigateAfterNetworkChange(previous, `${base}/user`); await expect(previous).toHaveURL(/\/login$/)
    await login(page)
    expect((await page.goto('/admin')).status()).toBe(200)
    await expect(page.getByRole('cell', { name: 'deployment.user', exact: true })).toBeVisible()
    await page.screenshot({ path: path.join(shots, '04-after-restart-admin.png'), fullPage: true })
    await fs.writeFile(path.join(out, 'restart-check.json'), JSON.stringify({ appContainerRecreated: true, databaseRestarted: true, existingAccountLogin: true, oldSessionRedirect: '/login', assignedAdminRolePersisted: true }, null, 2))
    await old.close()
  })
  test('logout clears the production JSESSIONID cookie', async ({ page }) => {
    await login(page)
    await page.getByRole('button', { name: 'Вийти', exact: true }).click()
    await expect(page).toHaveURL(`${base}/`)
    expect((await page.context().cookies()).find(c => c.name === 'JSESSIONID')).toBeUndefined()
    await page.goto('/user'); await expect(page).toHaveURL(/\/login$/)
  })
}
