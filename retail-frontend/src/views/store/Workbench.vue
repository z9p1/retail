<template>
  <div class="workbench">
    <h2>工作台</h2>
    <div v-if="zeroStockList.length" class="alert alert-restock">
      <strong>补货提醒</strong>：以下商品库存为 0，请及时补货
      <ul>
        <li v-for="p in zeroStockList" :key="p.id">{{ p.name }}（ID: {{ p.id }}）</li>
      </ul>
    </div>
    <div class="cards">
      <div class="card">
        <span class="label">待发货订单</span>
        <span class="value">{{ data.pendingShipCount ?? '-' }}</span>
      </div>
      <div class="card">
        <span class="label">低库存商品(&lt;5)</span>
        <span class="value">{{ data.lowStockCount ?? '-' }}</span>
      </div>
    </div>
    <p class="tip">流量与销售数据请到「流量监控」查看</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getWorkbench } from '../../api/workbench'

const data = ref({})

const zeroStockList = computed(() => {
  const list = data.value?.zeroStockProducts
  return Array.isArray(list) ? list : []
})

onMounted(async () => {
  try {
    data.value = await getWorkbench()
  } catch (e) {
    console.error(e)
  }
})
</script>

<style scoped>
.workbench h2 { margin: 0 0 1rem; }
.cards { display: flex; gap: 1rem; flex-wrap: wrap; }
.card { background: #16213e; padding: 1.25rem; border-radius: 8px; min-width: 160px; }
.card .label { display: block; color: #888; font-size: 0.9rem; }
.card .value { font-size: 1.5rem; color: #00d9ff; }
.tip { margin-top: 1.5rem; color: #666; }
.alert { padding: 1rem 1.25rem; border-radius: 8px; margin-bottom: 1rem; }
.alert-restock { background: #3d1a1a; border: 1px solid #e94560; color: #ffb3b3; }
.alert-restock ul { margin: 0.5rem 0 0 1.25rem; padding: 0; }
.alert-restock li { margin: 0.25rem 0; }
</style>
