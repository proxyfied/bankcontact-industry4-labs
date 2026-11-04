<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { customersApi, errorMessage, formatDate } from '../api'
import CustomerForm from '../components/CustomerForm.vue'
import AppDialog from '../components/AppDialog.vue'
const route = useRoute(), router = useRouter()
const records = ref([]), total = ref(0), pages = ref(0), page = ref(0)
const loading = ref(false), saving = ref(false), error = ref(''), formError = ref(''), notice = ref('')
const showForm = ref(false), editing = ref(null), deleting = ref(null)
function load() {
  loading.value = true; error.value = ''
  return customersApi.list(page.value).then(data => {
    records.value = data.content; total.value = data.totalElements; pages.value = data.totalPages
  }).catch(err => { error.value = errorMessage(err); records.value = [] })
    .finally(() => { loading.value = false })
}
function openForm(customer = null) { editing.value = customer; formError.value = ''; showForm.value = true; notice.value = '' }
function save(payload) {
  saving.value = true; formError.value = ''
  const update = Boolean(editing.value)
  const request = update ? customersApi.update(editing.value.id, payload) : customersApi.create(payload)
  request.then(customer => {
    showForm.value = false
    notice.value = update ? `Зміни збережено: ${customer.displayName}.` : `Клієнта ${customer.displayName} успішно додано.`
    if (!update) page.value = 0
    return load()
  }).catch(err => { formError.value = errorMessage(err) })
    .finally(() => { saving.value = false })
}
function remove() {
  saving.value = true; formError.value = ''
  const customer = deleting.value
  customersApi.remove(customer.id).then(() => {
    deleting.value = null; notice.value = `Клієнта ${customer.displayName} видалено.`
    if (records.value.length === 1 && page.value > 0) page.value--
    return load()
  }).catch(err => { formError.value = errorMessage(err) })
    .finally(() => { saving.value = false })
}
function go(delta) { page.value += delta; notice.value = ''; load() }
onMounted(() => {
  load()
  if (route.query.create === '1') { openForm(); router.replace('/customers') }
})
</script>
<template>
<div class="eyebrow">РОБОЧИЙ ПРОСТІР / КЛІЄНТИ</div>
<div class="page-heading"><div><h1>Клієнтська база<span class="accent-dot">.</span></h1><p>Керуйте контактами та зберігайте дані актуальними.</p></div><button class="btn btn-primary" @click="openForm()">＋ Додати клієнта</button></div>
<div v-if="notice" class="alert alert-success d-flex justify-content-between gap-3" role="status">{{ notice }}<button class="close-button" aria-label="Приховати повідомлення" @click="notice = ''">×</button></div>
<div v-if="error" class="alert alert-danger" role="alert">{{ error }} <button class="btn btn-sm btn-outline-danger ms-2" @click="load">Спробувати ще раз</button></div>
<section class="card section-card">
<div class="section-heading"><div class="d-flex align-items-center gap-2"><h2 class="mb-0">Усі клієнти</h2><span v-if="!error" class="count-badge">{{ total }}</span></div><button class="btn btn-sm btn-outline-secondary" :disabled="loading" @click="load">{{ loading ? 'Завантаження…' : '↻ Оновити' }}</button></div>
<div class="table-responsive"><table class="table align-middle mb-0"><thead><tr><th scope="col">Клієнт</th><th scope="col">Код клієнта</th><th scope="col">Додано</th><th scope="col">Оновлено</th><th scope="col" class="text-end">Дії</th></tr></thead><tbody>
<tr v-for="c in records" :key="c.id"><td><span class="name-cell"><span class="initial">{{ c.displayName.slice(0,1) }}</span><span>{{ c.displayName }}<small>ID {{ c.id }}</small></span></span></td><td><span class="code-tag">{{ c.externalCode }}</span></td><td class="muted nowrap">{{ formatDate(c.createdAt) }}</td><td class="muted nowrap">{{ formatDate(c.updatedAt) }}</td><td class="text-end"><div class="row-actions"><button class="btn btn-sm btn-edit" :aria-label="`Редагувати ${c.displayName}`" @click="openForm(c)">Редагувати</button><button class="btn btn-sm btn-delete" :aria-label="`Видалити ${c.displayName}`" @click="deleting = c; formError = ''; notice = ''">Видалити</button></div></td></tr>
<tr v-if="!records.length"><td colspan="5" class="empty-cell">{{ loading ? 'Завантажуємо клієнтів…' : error ? 'Не вдалося отримати список' : 'Клієнтів ще немає. Додайте перший запис.' }}</td></tr>
</tbody></table></div>
<div class="section-bottom d-flex flex-wrap justify-content-between align-items-center gap-3"><span>{{ error ? 'Список недоступний' : `Записів: ${total} · по 10 на сторінці` }}</span><div class="d-flex align-items-center gap-3"><button class="btn btn-sm btn-light" :disabled="loading || page === 0" @click="go(-1)">← Назад</button><span>Сторінка {{ page + 1 }} з {{ Math.max(1, pages) }}</span><button class="btn btn-sm btn-light" :disabled="loading || page + 1 >= pages" @click="go(1)">Далі →</button></div></div>
</section>
<div class="help-note"><span class="help-symbol">i</span><p>Контакти пов’язані зі зверненнями. Система зберігає історію обслуговування та захищає такі записи від видалення.</p></div>
<AppDialog v-if="showForm" :title="editing ? 'Редагування клієнта' : 'Новий клієнт'" :busy="saving" @close="showForm = false"><CustomerForm :customer="editing" :busy="saving" :error="formError" @save="save" @cancel="showForm = false" /></AppDialog>
<AppDialog v-if="deleting" title="Видалити клієнта?" :busy="saving" @close="deleting = null"><p class="muted">Клієнта <strong class="text-dark">{{ deleting.displayName }}</strong> буде видалено з бази. Цю дію неможливо скасувати.</p><div v-if="formError" class="alert alert-danger" role="alert">{{ formError }}</div><div class="d-flex gap-2 justify-content-end mt-4"><button class="btn btn-light" :disabled="saving" @click="deleting = null">Скасувати</button><button class="btn btn-danger" :disabled="saving" @click="remove">{{ saving ? 'Видалення…' : 'Підтвердити видалення' }}</button></div></AppDialog>
</template>