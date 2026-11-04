<script setup>
defineProps({ label: String, value: [String, Number], note: String, kind: String, number: String })
</script>
<template><div class="card stat-card h-100" :class="kind"><div class="d-flex justify-content-between"><span class="stat-label">{{ label }}</span><span class="stat-number">{{ number }}</span></div><div class="stat-value">{{ value ?? '—' }}</div><div class="stat-note">{{ note }}</div></div></template>