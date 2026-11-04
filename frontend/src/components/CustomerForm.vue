<script setup>
import { reactive, ref } from 'vue'
const props = defineProps({ customer: Object, busy: Boolean, error: String })
const emit = defineEmits(['save', 'cancel'])
const form = reactive({ externalCode: props.customer?.externalCode || '', displayName: props.customer?.displayName || '' })
const formElement = ref(null)
function submit() {
  if (!formElement.value.reportValidity()) return
  emit('save', { externalCode: form.externalCode.trim(), displayName: form.displayName.trim() })
}
</script>
<template>
<form ref="formElement" @submit.prevent="submit">
  <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
  <div class="mb-4"><label class="form-label" for="customer-code">Код клієнта <span aria-hidden="true">*</span></label><input id="customer-code" v-model="form.externalCode" class="form-control" maxlength="32" required pattern=".*\S.*" :disabled="busy" placeholder="Наприклад, CUST-003" autofocus><div class="form-text">Унікальний код у системі. До 32 символів.</div></div>
  <div class="mb-4"><label class="form-label" for="customer-name">Ім’я клієнта <span aria-hidden="true">*</span></label><input id="customer-name" v-model="form.displayName" class="form-control" maxlength="120" required pattern=".*\S.*" :disabled="busy" placeholder="Ім’я та прізвище"><div class="form-text">Відображатиметься у зверненнях. До 120 символів.</div></div>
  <div class="d-flex justify-content-end gap-2 border-top pt-4"><button type="button" class="btn btn-light" :disabled="busy" @click="emit('cancel')">Скасувати</button><button class="btn btn-primary" type="submit" :disabled="busy">{{ busy ? 'Збереження…' : customer ? 'Зберегти зміни' : 'Додати клієнта' }}</button></div>
</form>
</template>