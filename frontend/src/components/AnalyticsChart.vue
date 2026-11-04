<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { Chart, BarController, BarElement, CategoryScale, LinearScale, PieController, ArcElement, Tooltip, Legend } from 'chart.js'
Chart.register(BarController, BarElement, CategoryScale, LinearScale, PieController, ArcElement, Tooltip, Legend)
const props = defineProps({ type: { type: String, required: true }, buckets: { type: Array, required: true }, title: { type: String, required: true } })
const canvas = ref(null)
let chart
const colors = ['#245e4d', '#85a875', '#d3b474', '#6c929a', '#af8f9b', '#a9b6b0']
function draw() {
  const data = {
    labels: props.buckets.map(b => b.label),
    datasets: [{ label: 'Звернень', data: props.buckets.map(b => b.count), backgroundColor: props.type === 'pie' ? colors : '#52816a', borderWidth: 0, borderRadius: props.type === 'bar' ? 5 : 0 }]
  }
  if (chart) { chart.data = data; chart.update('none'); return }
  chart = new Chart(canvas.value, {
    type: props.type, data,
    options: {
      responsive: true, maintainAspectRatio: false, animation: false,
      plugins: { legend: { display: props.type === 'pie', position: 'bottom', labels: { usePointStyle: true, padding: 20, color: '#405950' } } },
      ...(props.type === 'bar' ? { scales: { y: { beginAtZero: true, ticks: { precision: 0 }, grid: { color: '#edf0ea' } }, x: { grid: { display: false }, ticks: { color: '#52685e' } } } } : {})
    }
  })
}
onMounted(draw)
watch(() => props.buckets, draw, { deep: true })
onBeforeUnmount(() => chart?.destroy())
</script>
<template><div class="bi-chart"><canvas ref="canvas" role="img" :aria-label="title">{{ title }}</canvas></div></template>
