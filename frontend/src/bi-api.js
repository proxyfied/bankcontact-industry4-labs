import axios from 'axios'

const bi = axios.create({
  baseURL: import.meta.env.VITE_BI_API_URL || 'http://localhost:8083/api',
  timeout: 30000
})
export const biApi = {
  statistics: () => bi.get('/analytics').then(r => r.data),
  upload: file => {
    const form = new FormData()
    form.append('file', file)
    return bi.post('/import/excel', form).then(r => r.data)
  },
  template: () => bi.get('/import/template', { responseType: 'blob' }).then(r => r.data)
}
export function biError(error) {
  return error.response?.data?.detail || (error.response
    ? `Помилка сервера (${error.response.status}). Спробуйте ще раз.`
    : 'Немає зв’язку з BI-сервісом. Перевірте підключення та повторіть запит.')
}
