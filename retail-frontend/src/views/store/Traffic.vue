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
    <section class="trend-section">
      <h3>近 7 天销量趋势（线型图）</h3>
      <div class="product-tabs">
        <button type="button" :class="{ active: selectedProductId === null }" @click="selectedProductId = null">全部</button>
        <button v-for="p in trendProducts" :key="p.productId" type="button" :class="{ active: selectedProductId === p.productId }" @click="selectedProductId = p.productId">{{ p.productName }}</button>
      </div>
      <div class="chart-wrap">
        <svg v-if="trend.dates && trend.dates.length" class="line-chart" viewBox="0 0 400 140" preserveAspectRatio="none">
          <defs>
            <linearGradient id="lineGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#00d9ff" stop-opacity="0.4" />
              <stop offset="100%" stop-color="#00d9ff" stop-opacity="0" />
            </linearGradient>
          </defs>
          <polyline
            fill="url(#lineGrad)"
            :points="areaChartPoints"
            stroke="none"
          />
          <polyline
            :points="lineChartPoints"
            fill="none"
            stroke="#00d9ff"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            vector-effect="non-scaling-stroke"
          />
        </svg>
        <div v-if="trend.dates && trend.dates.length" class="chart-labels">
          <span v-for="d in trend.dates" :key="d" class="chart-label">{{ d.slice(5) }}</span>
        </div>
        <p v-else class="no-data">暂无数据</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getTraffic, getTrafficTrend } from '../../api/traffic'

const range = ref('7')
const data = ref({})
const trend = ref({})
const selectedProductId = ref(null)

const trendProducts = computed(() => trend.value.products || [])
const currentAmounts = computed(() => {
  if (selectedProductId.value == null) return (trend.value.dailyTotalAmounts || []).map(Number)
  const p = trendProducts.value.find(x => x.productId === selectedProductId.value)
  return p ? (p.dailyAmounts || []).map(Number) : []
})
const maxAmount = computed(() => {
  const arr = currentAmounts.value
  return Math.max(1, ...arr)
})
const padding = { left: 20, right: 20, top: 10, bottom: 24 }
const chartW = 400 - padding.left - padding.right
const chartH = 140 - padding.top - padding.bottom
const lineChartPoints = computed(() => {
  const arr = currentAmounts.value
  const max = maxAmount.value
  if (!arr.length) return ''
  const step = arr.length <= 1 ? chartW : chartW / (arr.length - 1)
  return arr.map((v, i) => {
    const x = padding.left + i * step
    const y = padding.top + chartH - (v / max) * chartH
    return `${x},${y}`
  }).join(' ')
})
const areaChartPoints = computed(() => {
  const arr = currentAmounts.value
  const max = maxAmount.value
  if (!arr.length) return ''
  const step = arr.length <= 1 ? chartW : chartW / (arr.length - 1)
  const pts = arr.map((v, i) => {
    const x = padding.left + i * step
    const y = padding.top + chartH - (v / max) * chartH
    return `${x},${y}`
  })
  const bottomY = padding.top + chartH
  const firstX = padding.left
  const lastX = padding.left + (arr.length <= 1 ? 0 : chartW)
  return `${firstX},${bottomY} ${pts.join(' ')} ${lastX},${bottomY}`
})

async function load() {
  try {
    data.value = await getTraffic(range.value)
  } catch (e) {
    console.error(e)
  }
}
async function loadTrend() {
  try {
    trend.value = await getTrafficTrend(7)
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => { load(); loadTrend() })
</script>

<style scoped>
.traffic h2 { margin: 0 0 1rem; }
.cards { display: flex; gap: 1rem; flex-wrap: wrap; }
.card { background: #16213e; padding: 1.25rem; border-radius: 8px; min-width: 120px; }
.card .label { display: block; color: #888; }
.card .value { font-size: 1.25rem; color: #00d9ff; }
.trend-section { margin-top: 2rem; }
.trend-section h3 { font-size: 1rem; margin-bottom: 0.75rem; }
.product-tabs { margin-bottom: 1rem; }
.product-tabs button { margin-right: 0.5rem; padding: 0.35rem 0.75rem; border: 1px solid #333; border-radius: 4px; background: #16213e; color: #ccc; cursor: pointer; }
.product-tabs button.active { background: #00d9ff; color: #1a1a2e; border-color: #00d9ff; }
.chart-wrap { background: #16213e; padding: 1rem; border-radius: 8px; min-height: 180px; }
.line-chart { width: 100%; height: 140px; display: block; }
.chart-labels { display: flex; justify-content: space-between; margin-top: 4px; padding: 0 4px; }
.chart-label { font-size: 0.7rem; color: #888; }
.no-data { color: #888; margin: 0; }
</style>
