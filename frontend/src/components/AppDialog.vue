<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
const props = defineProps({ title: String, busy: Boolean })
const emit = defineEmits(['close'])
const panel = ref(null)
let previousFocus
function keydown(event) {
  if (event.key === 'Escape' && !props.busy) emit('close')
  if (event.key !== 'Tab') return
  const items = [...panel.value.querySelectorAll('button:not([disabled]),input:not([disabled]),[tabindex="0"]')]
  if (!items.length) { event.preventDefault(); return }
  if (event.shiftKey && document.activeElement === items[0]) { event.preventDefault(); items.at(-1).focus() }
  else if (!event.shiftKey && document.activeElement === items.at(-1)) { event.preventDefault(); items[0].focus() }
}
onMounted(() => {
  previousFocus = document.activeElement
  document.body.style.overflow = 'hidden'
  panel.value.querySelector('input,button')?.focus()
  document.addEventListener('keydown', keydown)
})
onBeforeUnmount(() => {
  document.body.style.overflow = ''
  document.removeEventListener('keydown', keydown)
  previousFocus?.focus()
})
</script>
<template><Teleport to="body"><div class="dialog-backdrop" @click.self="!busy && emit('close')"><section ref="panel" class="dialog-panel" role="dialog" aria-modal="true" aria-labelledby="dialog-title"><div class="d-flex justify-content-between align-items-start mb-4"><h2 id="dialog-title" class="h4 mb-0">{{ title }}</h2><button class="close-button" type="button" aria-label="Закрити" :disabled="busy" @click="emit('close')">×</button></div><slot /></section></div></Teleport></template>