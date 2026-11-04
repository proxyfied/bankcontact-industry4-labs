import axios from 'axios'
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  headers: { Accept: 'application/json' }
})
export function errorMessage(error) {
  if (!error.response) return 'Немає зв’язку із сервером. Перевірте підключення та спробуйте ще раз.'
  const status = error.response.status
  if (status === 409) return 'Конфлікт даних: код клієнта вже існує або запис має пов’язані звернення.'
  if (status === 404) return 'Запис більше не існує. Оновіть список клієнтів.'
  if (status === 400) return 'Перевірте обов’язкові поля та довжину значень.'
  return 'Не вдалося виконати запит. Спробуйте ще раз.'
}
export const customersApi = {
  list: (page = 0, size = 10) => api.get('/customers', { params: { page, size } }).then(r => r.data),
  create: data => api.post('/customers', data).then(r => r.data),
  update: (id, data) => api.put(`/customers/${id}`, data).then(r => r.data),
  remove: id => api.delete(`/customers/${id}`)
}
export const formatDate = value => value ? new Intl.DateTimeFormat('uk-UA', {
  day: '2-digit', month: 'short', year: 'numeric', timeZone: 'Europe/Kyiv'
}).format(new Date(value)) : '—'
