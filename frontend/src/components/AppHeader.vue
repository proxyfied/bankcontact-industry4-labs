<script setup>
const articleAiUrl = import.meta.env.VITE_ARTICLE_AI_URL || 'http://localhost:8082/articles'
</script>
<template>
<header class="app-header">
  <div class="container header-inner">
    <RouterLink to="/" class="brand" aria-label="BankContact — головна">
      <span class="brand-symbol" aria-hidden="true"><svg viewBox="0 0 32 32"><path d="M7 17v-2a9 9 0 0 1 18 0v6c0 4-4 6-8 6M7 15h4v8H7zm14 0h4v8h-4z"/></svg></span>
      <span>Bank<span class="brand-light">Contact</span><small>КОНТАКТ-ЦЕНТР</small></span>
    </RouterLink>
    <nav aria-label="Головне меню" class="top-nav">
      <RouterLink to="/" exact-active-class="active">Огляд</RouterLink>
      <RouterLink to="/customers" active-class="active">Клієнти</RouterLink>
      <RouterLink to="/about" active-class="active">Про проєкт</RouterLink>
      <a :href="articleAiUrl">AI статті ↗</a>
    </nav>
    <div class="workspace"><span class="workspace-dot"></span>Навчальний простір<span class="avatar">ЄМ</span></div>
  </div>
</header>
</template>
