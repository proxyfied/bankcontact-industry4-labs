<script setup>
import { onMounted, ref } from 'vue'
import { api, errorMessage, formatDate } from '../api'
import StatCard from '../components/StatCard.vue'
const busy = ref(false), error = ref(''), data = ref(null)
function load() {
  busy.value = true; error.value = ''
  Promise.all(['customers', 'agents', 'tickets'].map(name => api.get(`/${name}`, { params: { size: 5 } })))
    .then(([customers, agents, tickets]) => { data.value = { customers: customers.data, agents: agents.data, tickets: tickets.data } })
    .catch(err => { data.value = null; error.value = errorMessage(err) })
    .finally(() => { busy.value = false })
}
onMounted(load)
</script>
<template>
<div class="eyebrow">РОБОЧИЙ ПРОСТІР / ОГЛЯД</div>
<div class="page-heading"><div><h1>Контакт починається тут<span class="accent-dot">.</span></h1><p>Клієнти, команда та звернення в одному просторі.</p></div><button class="btn btn-outline-secondary" :disabled="busy" @click="load">{{ busy ? 'Оновлення…' : '↻ Оновити дані' }}</button></div>
<div v-if="error" class="alert alert-danger d-flex flex-wrap justify-content-between gap-2" role="alert">{{ error }}<button class="btn btn-sm btn-outline-danger" @click="load">Спробувати ще раз</button></div>
<div class="row g-4 mb-4">
<div class="col-md-4"><StatCard label="Клієнти" :value="data?.customers.totalElements" note="Записи в клієнтській базі" kind="featured" number="01" /></div>
<div class="col-md-4"><StatCard label="Оператори" :value="data?.agents.totalElements" note="Команда контакт-центру" number="02" /></div>
<div class="col-md-4"><StatCard label="Звернення" :value="data?.tickets.totalElements" note="Зареєстровані запити клієнтів" number="03" /></div>
</div>
<div class="row g-4">
<div class="col-lg-8"><section class="card section-card h-100"><div class="section-heading"><div><span class="eyebrow">КЛІЄНТСЬКА БАЗА</span><h2>Клієнти під рукою</h2></div><RouterLink to="/customers" class="text-link">Усі клієнти ↗</RouterLink></div><div class="table-responsive"><table class="table align-middle mb-0"><thead><tr><th scope="col">Клієнт</th><th scope="col">Код</th><th scope="col">Дата додавання</th></tr></thead><tbody><tr v-for="c in data?.customers.content" :key="c.id"><td><span class="name-cell"><span class="initial">{{ c.displayName.slice(0,1) }}</span>{{ c.displayName }}</span></td><td><span class="code-tag">{{ c.externalCode }}</span></td><td class="muted">{{ formatDate(c.createdAt) }}</td></tr><tr v-if="!data?.customers.content.length"><td colspan="3" class="empty-cell">{{ busy ? 'Завантаження даних…' : error ? 'Дані недоступні' : 'Клієнтів ще немає' }}</td></tr></tbody></table></div><div class="section-bottom">Перші 5 записів · дані з корпоративної системи</div></section></div>
<div class="col-lg-4"><section class="card action-card h-100"><div class="action-icon" aria-hidden="true">↗</div><span class="eyebrow">НАСТУПНИЙ КРОК</span><h2>Більше уваги<br>кожному клієнту</h2><p>Додавайте нові контакти та підтримуйте клієнтську базу актуальною.</p><RouterLink to="/customers?create=1" class="btn btn-dark mt-auto">Додати клієнта <span>＋</span></RouterLink></section></div>
</div>
<div class="system-strip"><span><span class="status-dot" :class="{ offline: error || !data }"></span>{{ busy ? 'Перевірка зв’язку…' : error ? 'Сервер недоступний' : data ? 'Дані синхронізовано' : 'Очікування даних' }}</span><span>Єдина база · узгоджені процеси · жодного зайвого кроку</span></div>
</template>