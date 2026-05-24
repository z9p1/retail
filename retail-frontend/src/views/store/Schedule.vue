<template>
  <div class="schedule">
    <UiPageHeader title="任务管理" description="配置模拟购买任务与智能助手的 Dify 应用。"/>

    <UiCard title="模拟购买" subtitle="用于演示与生成订单数据。">
      <div class="task-row">
        <div class="task-desc">{{ data.description || 'customer1 每小时自动购买一件在售商品（模拟购买）' }}</div>
        <UiSwitch v-model="enabled" @update:modelValue="toggle" />
      </div>
      <p class="tip">开启后，系统将每隔 1 小时以 customer1 身份自动下一单（任选一件有库存在售商品，数量 1）并完成支付，用于模拟购买数据。</p>
    </UiCard>

    <div class="spacer"></div>

    <UiCard title="智能助手" subtitle="选择后，智能助手对话将调用该 Dify 应用。">
      <div class="task-row">
        <div class="task-desc">智能助手使用的 Dify 应用</div>
        <select v-model="difyAppCurrent" @change="onDifyAppChange" class="dify-select">
          <option value="">未配置</option>
          <option v-for="opt in difyAppOptions" :key="opt" :value="opt">{{ opt }}</option>
        </select>
      </div>
      <p class="tip">应用名对应你在 Dify 工作室中创建的应用标识。</p>
    </UiCard>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getSimulatePurchase, setSimulatePurchaseEnabled } from '../../api/schedule'
import { getDifyAppConfig, setDifyAppCurrent as setDifyAppApi } from '../../api/agentConfig'
import UiPageHeader from '../../components/ui/UiPageHeader.vue'
import UiCard from '../../components/ui/UiCard.vue'
import UiSwitch from '../../components/ui/UiSwitch.vue'
import { useToast } from '../../components/ui/toast'

const toast = useToast()

const data = ref({})
const enabled = ref(false)
const difyAppOptions = ref([])
const difyAppCurrent = ref('')

async function load() {
  try {
    data.value = await getSimulatePurchase()
    enabled.value = data.value.enabled === true
  } catch (e) {
    console.error(e)
  }
  try {
    const res = await getDifyAppConfig()
    difyAppOptions.value = res.options || []
    difyAppCurrent.value = res.current != null ? res.current : ''
  } catch (e) {
    console.error(e)
  }
}

async function toggle() {
  try {
    await setSimulatePurchaseEnabled(enabled.value)
  } catch (e) {
    enabled.value = !enabled.value
    toast.error(e.message || '操作失败')
  }
}

async function onDifyAppChange() {
  try {
    await setDifyAppApi(difyAppCurrent.value)
    toast.success('已保存')
  } catch (e) {
    toast.error(e.message || '保存失败')
  }
}

onMounted(load)
</script>

<style scoped>
.task-row { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); }
.task-desc { flex: 1; color: var(--text); font-weight: 700; }
.tip { margin: var(--space-3) 0 0; color: var(--muted); font-size: 13px; line-height: 1.6; }
.dify-select { min-width: 160px; }
.spacer { height: var(--space-4); }
</style>
