<template>
  <div class="schedule">
    <h2>任务管理</h2>
    <div class="task-card">
      <div class="task-row">
        <span class="task-desc">{{ data.description || 'customer1 每小时自动购买一件在售商品（模拟购买）' }}</span>
        <label class="switch">
          <input type="checkbox" v-model="enabled" @change="toggle" />
          <span class="slider"></span>
        </label>
      </div>
      <p class="tip">开启后，系统将每隔 1 小时以 customer1 身份自动下一单（任选一件有库存在售商品，数量 1）并完成支付，用于模拟购买数据。</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getSimulatePurchase, setSimulatePurchaseEnabled } from '../../api/schedule'

const data = ref({})
const enabled = ref(false)

async function load() {
  try {
    data.value = await getSimulatePurchase()
    enabled.value = data.value.enabled === true
  } catch (e) {
    console.error(e)
  }
}

async function toggle() {
  try {
    await setSimulatePurchaseEnabled(enabled.value)
  } catch (e) {
    enabled.value = !enabled.value
    alert(e.message || '操作失败')
  }
}

onMounted(load)
</script>

<style scoped>
.schedule h2 { margin: 0 0 1rem; }
.task-card { background: #16213e; padding: 1.25rem; border-radius: 8px; max-width: 560px; }
.task-row { display: flex; align-items: center; justify-content: space-between; gap: 1rem; }
.task-desc { flex: 1; color: #fff; }
.tip { margin: 1rem 0 0; color: #888; font-size: 0.9rem; }
.switch { position: relative; display: inline-block; width: 48px; height: 26px; flex-shrink: 0; }
.switch input { opacity: 0; width: 0; height: 0; }
.slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background: #333; border-radius: 26px; transition: 0.3s; }
.slider::before { position: absolute; content: ""; height: 20px; width: 20px; left: 3px; bottom: 3px; background: #eee; border-radius: 50%; transition: 0.3s; }
input:checked + .slider { background: #00d9ff; }
input:checked + .slider::before { transform: translateX(22px); }
</style>
