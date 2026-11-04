<script setup>
import { ref, computed, onMounted } from 'vue'
import { biApi, biError } from '../bi-api'
import AnalyticsChart from '../components/AnalyticsChart.vue'
const selected = ref(null), fileInput = ref(null), stats = ref(null)
const loading = ref(false), uploading = ref(false), downloading = ref(false)
const error = ref(''), notice = ref(''), fileError = ref('')
const busy = computed(() => loading.value || uploading.value)
const statusNames = { NEW: 'Нові', IN_PROGRESS: 'У роботі', RESOLVED: 'Вирішені', CLOSED: 'Закриті' }
const statuses = computed(() => (stats.value?.statuses || []).map(b => ({ ...b, label: statusNames[b.label] || b.label })))
const resolved = computed(() => (stats.value?.statuses || []).filter(b => ['RESOLVED', 'CLOSED'].includes(b.label)).reduce((sum, b) => sum + b.count, 0))
const share = count => stats.value?.total ? `${(100 * count / stats.value.total).toFixed(1)}%` : '0.0%'
function choose(event) {
  selected.value = event.target.files?.[0] || null; fileError.value = ''; notice.value = ''
  if (selected.value && (!selected.value.name.toLowerCase().endsWith('.xlsx') || selected.value.size > 2 * 1024 * 1024)) {
    fileError.value = 'Оберіть файл .xlsx розміром до 2 МБ.'; selected.value = null; event.target.value = ''
  }
}
function load() {
  loading.value = true; error.value = ''; stats.value = null
  return biApi.statistics().then(data => { stats.value = data })
    .catch(err => { error.value = biError(err) }).finally(() => { loading.value = false })
}
function upload() {
  if (!selected.value || busy.value) return
  uploading.value = true; error.value = ''; notice.value = ''; fileError.value = ''
  biApi.upload(selected.value).then(async result => {
    notice.value = `Імпортовано: ${result.imported} записів. Пропущено порожніх рядків: ${result.skipped}.`
    selected.value = null; fileInput.value.value = ''
    await load()
  }).catch(err => { fileError.value = biError(err) })
    .finally(() => { uploading.value = false })
}
function download() {
  downloading.value = true; fileError.value = ''
  biApi.template().then(blob => {
    const url = URL.createObjectURL(blob), a = document.createElement('a')
    a.href = url; a.download = 'template.xlsx'; a.click(); setTimeout(() => URL.revokeObjectURL(url), 1000)
  }).catch(err => { fileError.value = biError(err) }).finally(() => { downloading.value = false })
}
onMounted(load)
</script>
<template>
<div class="eyebrow">РОБОЧИЙ ПРОСТІР / BI АНАЛІТИКА</div>
<div class="page-heading"><div><h1>Аналітика звернень<span class="accent-dot">.</span></h1><p>Імпортуйте Excel і переглядайте розподіл звернень контакт-центру.</p></div><button class="btn btn-outline-secondary" :disabled="busy" @click="load">{{ loading ? 'Завантаження…' : '↻ Оновити дані' }}</button></div>
<section class="card section-card bi-import mb-4">
  <div class="bi-import-heading"><div><span class="eyebrow">01 / ДЖЕРЕЛО ДАНИХ</span><h2>Імпорт із Excel</h2><p class="muted mb-0">До 2000 рядків · .xlsx · до 2 МБ</p></div><button class="btn btn-light" :disabled="downloading" @click="download">{{ downloading ? 'Завантаження…' : '↓ Завантажити шаблон' }}</button></div>
  <form class="bi-upload-form" @submit.prevent="upload"><div class="flex-grow-1"><label for="excel-file" class="form-label">Файл зі зверненнями</label><input id="excel-file" ref="fileInput" type="file" accept=".xlsx" class="form-control" :disabled="busy" @change="choose"></div><button type="submit" class="btn btn-primary" :disabled="!selected || busy">{{ uploading ? 'Імпортуємо…' : 'Імпортувати' }}</button></form>
  <p class="bi-schema">Код · Тема · Категорія · Статус · Дата · Тривалість хв</p>
  <p class="bi-hint">Статуси: NEW, IN_PROGRESS, RESOLVED, CLOSED. Дата: РРРР-ММ-ДД або дата Excel. Кожен код має бути унікальним. За помилки імпорт скасовується повністю.</p>
  <div v-if="fileError" class="alert alert-danger mb-0 mt-3" role="alert">{{ fileError }}</div>
  <div v-if="notice" class="alert alert-success mb-0 mt-3" role="status">{{ notice }}</div>
</section>
<div v-if="error" class="alert alert-danger" role="alert">{{ error }} <button class="btn btn-sm btn-outline-danger ms-2" :disabled="busy" @click="load">Повторити</button></div>
<p v-if="loading" class="muted" role="status">Отримуємо статистику з PostgreSQL…</p>
<template v-if="stats">
<div class="row g-3 mb-4 bi-kpis"><div class="col-md-4"><div class="card bi-kpi"><span>Усього звернень</span><strong data-testid="bi-total">{{ stats.total }}</strong><small>Усі імпортовані записи</small></div></div><div class="col-md-4"><div class="card bi-kpi"><span>Вирішені та закриті</span><strong>{{ share(resolved) }}</strong><small>{{ resolved }} із {{ stats.total }} звернень</small></div></div><div class="col-md-4"><div class="card bi-kpi"><span>Тривалість обробки</span><strong>{{ stats.totalMinutes }} <em>хв</em></strong><small>Сума з імпортованих рядків</small></div></div></div>
<div v-if="!stats.total" class="card bi-empty"><span class="eyebrow">ДАНИХ ЩЕ НЕМАЄ</span><h2>Завантажте першу таблицю</h2><p>Після успішного імпорту тут з’являться діаграми за категоріями та статусами.</p></div>
<template v-else>
<div class="bi-period">Статистика за всіма імпортами. Повторне завантаження тих самих кодів відхиляється.</div>
<div class="row g-4 mb-4"><div class="col-lg-7"><section class="card section-card h-100"><span class="eyebrow">02 / КАТЕГОРІЇ</span><h2>Кількість звернень</h2><AnalyticsChart type="bar" :buckets="stats.categories" title="Кількість звернень за категоріями"/><div class="bi-buckets"><span v-for="b in stats.categories" :key="b.label">{{ b.label }} <strong>{{ b.count }}</strong></span></div></section></div><div class="col-lg-5"><section class="card section-card h-100"><span class="eyebrow">03 / СТАТУСИ</span><h2>Структура обслуговування</h2><AnalyticsChart type="pie" :buckets="statuses" title="Розподіл звернень за статусами"/><div class="bi-buckets"><span v-for="b in statuses" :key="b.label">{{ b.label }} <strong>{{ share(b.count) }}</strong></span></div></section></div></div>
<section class="card section-card"><div class="section-heading"><div><span class="eyebrow">04 / ДИНАМІКА</span><h2>Звернення за датами</h2></div><span class="count-badge">{{ stats.dates.length }} дат</span></div><div class="table-responsive"><table class="table mb-0"><thead><tr><th>Дата реєстрації</th><th class="text-end">Кількість</th><th class="text-end">Частка</th></tr></thead><tbody><tr v-for="b in stats.dates" :key="b.label"><td>{{ b.label }}</td><td class="text-end">{{ b.count }}</td><td class="text-end">{{ share(b.count) }}</td></tr></tbody></table></div></section>
</template>
</template>
</template>
