const form = document.getElementById('generate-form')
const message = document.getElementById('message')
const counter = document.getElementById('counter')
const errorBox = document.getElementById('error')
const result = document.getElementById('result')
const empty = document.getElementById('empty')
const clock = document.getElementById('clock')
const submit = document.getElementById('submit')
const output = document.getElementById('output')
let lastJson = '', busy = false
const sample = 'Лозовик Ю.М. Розвиток штучного інтелекту в економіці. Журнал "Цифрова Економіка", Том 5, Випуск 2, 2025 рік, Київ, стор. 10-20. DOI: 10.1234/example.'
message.addEventListener('input', () => { counter.textContent = `${message.value.length} / 12000` })
document.getElementById('example').addEventListener('click', () => {
  message.value = sample; message.dispatchEvent(new Event('input')); message.focus()
})
document.getElementById('clear').addEventListener('click', () => {
  if (busy) return
  form.reset(); message.dispatchEvent(new Event('input')); result.hidden = true
  empty.hidden = false; errorBox.hidden = true; clock.textContent = '— мс'; lastJson = ''; message.focus()
})
function setBusy(value) {
  busy = value
  for (const id of ['submit', 'example', 'clear', 'message']) document.getElementById(id).disabled = value
  output.setAttribute('aria-busy', String(value))
  submit.textContent = value ? 'Опрацювання…' : 'Структурувати статтю ↗'
}
form.addEventListener('submit', async event => {
  event.preventDefault()
  if (busy) return
  if (!message.value.trim()) { message.setCustomValidity('Введіть текст цитування.'); message.reportValidity(); message.setCustomValidity(''); return }
  setBusy(true); errorBox.hidden = true; result.hidden = true; empty.hidden = false
  const started = performance.now(), controller = new AbortController()
  const timer = setInterval(() => { clock.textContent = `${Math.round(performance.now() - started)} мс…` }, 100)
  const timeout = setTimeout(() => controller.abort(), 55000)
  try {
    const response = await fetch('/generate-all', {
      method: 'POST', headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ message: message.value }), signal: controller.signal
    })
    const data = await response.json()
    if (!response.ok) throw new Error(data.detail || 'Сервер не зміг опрацювати запит.')
    render(data)
  } catch (error) {
    errorBox.textContent = error.name === 'AbortError' ? 'Час очікування вичерпано. Спробуйте ще раз.' : error instanceof TypeError ? 'Немає зв’язку із сервером. Перевірте підключення.' : error.message
    errorBox.hidden = false; clock.textContent = `${Math.round(performance.now() - started)} мс · помилка`
  } finally {
    clearInterval(timer); clearTimeout(timeout); setBusy(false)
  }
})
function render(data) {
  const article = data.article
  document.getElementById('article-title').textContent = article.title_of_the_article || 'Без назви'
  document.getElementById('article-author').textContent = article.author_set || 'Авторів не вказано'
  document.getElementById('pages').textContent = article.total_volume_of_printed_sheets
  document.getElementById('language').textContent = article.language || '—'
  document.getElementById('year').textContent = article.year_of_publication
  const fields = document.getElementById('fields'); fields.replaceChildren()
  for (const [key, value] of Object.entries(article)) {
    const tr = document.createElement('tr'), name = document.createElement('td'), content = document.createElement('td')
    name.textContent = key; content.textContent = value || '—'; tr.append(name, content); fields.append(tr)
  }
  lastJson = JSON.stringify(article, null, 2)
  document.getElementById('json').textContent = lastJson
  clock.textContent = `${data.clock} мс · сервер`
  empty.hidden = true; result.hidden = false
}
document.getElementById('copy').addEventListener('click', async event => {
  try { await navigator.clipboard.writeText(lastJson); event.target.textContent = 'Скопійовано ✓' }
  catch { event.target.textContent = 'JSON доступний нижче' }
})
