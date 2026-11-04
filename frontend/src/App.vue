<script setup>
import AppHeader from './components/AppHeader.vue'
import AppFooter from './components/AppFooter.vue'
</script>
<template>
  <a class="skip-link" href="#main">Перейти до вмісту</a>
  <AppHeader />
  <main id="main" class="container app-main"><RouterView /></main>
  <AppFooter />
</template>
