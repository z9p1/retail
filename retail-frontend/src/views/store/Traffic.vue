<template>
  <div class="traffic">
    <h2>流量监控</h2>
    <div class="toolbar">
      <select v-model="range" @change="load">
        <option value="today">今日</option>
        <option value="7">最近7天</option>
        <option value="30">最近30天</option>
      </select>
    </div>
    <div class="cards">
      <div class="card"><span class="label">UV</span><span class="value">{{ data.uv ?? '-' }}</span></div>
      <div class="card"><span class="label">PV</span><span class="value">{{ data.pv ?? '-' }}</span></div>
      <div class="card"><span class="label">下单笔数</span><span class="value">{{ data.orderCount ?? '-' }}</span></div>
      <div class="card"><span class="label">下单人数</span><span class="value">{{ data.userCount ?? '-' }}</span></div>
      <div class="card"><span class="label">成交金额</span><span class="value">{{ data.amount ?? '-' }}</span></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getTraffic } from '../../api/traffic'

const range = ref('7')
const data = ref({})

async function load() {
  try {
    data.value = await getTraffic(range.value)
  } catch (e) {
    console.error(e)
  }
}

onMounted(load)
</script>

<style scoped>
.traffic h2 { margin: 0 0 1rem; }
.cards { display: flex; gap: 1rem; flex-wrap: wrap; }
.card { background: #16213e; padding: 1.25rem; border-radius: 8px; min-width: 120px; }
.card .label { display: block; color: #888; }
.card .value { font-size: 1.25rem; color: #00d9ff; }
</style>
