<template>
  <div class="user-analysis">
    <UiPageHeader title="用户消费分析" description="查询消费者并查看订单与商品偏好。"/>

    <UiCard title="快捷选择" subtitle="点击消费者名称可自动填入搜索框。">
      <div class="consumers-bar">
        <span v-if="!consumers.length" class="bar-empty">暂无消费者</span>
        <button
          v-for="u in consumers"
          :key="u.id"
          type="button"
          class="consumer-pill"
          @click="fillAndSearch(u)"
        >{{ u.nickname || u.username }}</button>
      </div>
    </UiCard>

    <div class="spacer"></div>

    <UiCard title="搜索用户" subtitle="支持按用户ID/手机/昵称搜索。">
      <div class="toolbar">
        <input ref="searchInputRef" v-model="keyword" placeholder="用户ID/手机/昵称" @keyup.enter="search" />
        <UiButton variant="primary" @click="search">搜索</UiButton>
      </div>
      <div v-if="userList.length" class="user-list">
        <div class="list-title">选择用户查看分析</div>
        <div class="list-actions">
          <UiButton v-for="u in userList" :key="u.id" variant="ghost" @click="loadAnalysis(u.id)">
            {{ u.nickname || u.username }} (ID:{{ u.id }})
          </UiButton>
        </div>
      </div>
    </UiCard>

    <div class="spacer"></div>

    <UiCard v-if="analysis" title="分析结果" subtitle="包含消费汇总、订单明细与商品偏好。">
      <div class="summary">
        <div class="metric">
          <div class="m-label">订单数</div>
          <div class="m-value">{{ analysis.orderCount }}</div>
        </div>
        <div class="metric">
          <div class="m-label">总金额</div>
          <div class="m-value">¥ {{ analysis.totalAmount }}</div>
        </div>
        <div class="metric">
          <div class="m-label">首次支付</div>
          <div class="m-value small">{{ analysis.firstPayTime || '—' }}</div>
        </div>
        <div class="metric">
          <div class="m-label">最近支付</div>
          <div class="m-value small">{{ analysis.lastPayTime || '—' }}</div>
        </div>
      </div>

      <div class="section-title">订单明细</div>
      <table class="table" v-if="analysis.orders && analysis.orders.length">
        <thead><tr><th>订单号</th><th>金额</th><th>状态</th><th>下单/支付时间</th></tr></thead>
        <tbody>
          <tr v-for="o in analysis.orders" :key="o.id">
            <td>{{ o.orderNo }}</td>
            <td>¥ {{ o.totalAmount }}</td>
            <td><UiTag :tone="statusTone(o.status)">{{ o.status }}</UiTag></td>
            <td>{{ o.createTime }} / {{ o.payTime || '—' }}</td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty">暂无订单</div>

      <div class="section-title">商品偏好</div>
      <table class="table" v-if="analysis.preference && analysis.preference.length">
        <thead><tr><th>商品</th><th>数量</th><th>金额</th></tr></thead>
        <tbody>
          <tr v-for="(pref, i) in analysis.preference" :key="i">
            <td>{{ pref.productName }}</td>
            <td>{{ pref.quantity }}</td>
            <td>¥ {{ pref.amount }}</td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty">暂无偏好数据</div>
    </UiCard>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listConsumers, searchUser, getUserAnalysis } from '../../api/userAnalysis'
import UiPageHeader from '../../components/ui/UiPageHeader.vue'
import UiCard from '../../components/ui/UiCard.vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiTag from '../../components/ui/UiTag.vue'
import { useToast } from '../../components/ui/toast'

const toast = useToast()

const searchInputRef = ref(null)
const keyword = ref('')
const consumers = ref([])
const userList = ref([])
const analysis = ref(null)

async function loadConsumers() {
  try {
    consumers.value = await listConsumers()
  } catch (e) {
    console.error(e)
  }
}

function fillAndSearch(u) {
  const name = u.nickname || u.username || ''
  keyword.value = name
  searchInputRef.value?.focus()
  if (name.trim()) search()
}

async function search() {
  if (!keyword.value.trim()) return
  try {
    userList.value = await searchUser(keyword.value)
  } catch (e) {
    console.error(e)
    toast.error(e.message || '搜索失败')
  }
}

async function loadAnalysis(userId) {
  try {
    analysis.value = await getUserAnalysis(userId)
  } catch (e) {
    console.error(e)
    toast.error(e.message || '加载失败')
  }
}

function statusTone(s) {
  const m = { PENDING_PAY: 'warning', CANCELLED: 'danger', PENDING_SHIP: 'warning', SHIPPED: 'info', COMPLETED: 'success' }
  return m[s] || 'neutral'
}

onMounted(loadConsumers)
</script>

<style scoped>
.spacer { height: var(--space-4); }

.consumers-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.consumer-pill {
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(37, 99, 235, 0.18);
  background: rgba(37, 99, 235, 0.08);
  color: var(--primary-2);
  cursor: pointer;
}
.consumer-pill:hover { background: rgba(37, 99, 235, 0.12); }
.bar-empty { color: var(--muted); }

.toolbar { display: flex; gap: var(--space-2); align-items: center; flex-wrap: wrap; }
.user-list { margin-top: var(--space-3); }
.list-title { font-weight: 800; color: var(--text); margin-bottom: var(--space-2); }
.list-actions { display: flex; gap: 8px; flex-wrap: wrap; }

.summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-3);
  padding: var(--space-3);
  border: 1px solid var(--border);
  background: var(--surface-2);
  border-radius: var(--radius-md);
}
.metric { min-width: 0; }
.m-label { color: var(--muted); font-size: 12px; }
.m-value { margin-top: 4px; font-weight: 900; color: var(--text); }
.m-value.small { font-size: 12px; font-weight: 700; color: var(--text-2); word-break: break-word; }

.section-title {
  margin-top: var(--space-4);
  margin-bottom: var(--space-2);
  font-weight: 900;
  color: var(--text);
}

.table { width: 100%; border-collapse: collapse; }
.table th, .table td { border-top: 1px solid var(--border); padding: 10px 12px; color: var(--text-2); }
.table th { background: var(--surface-2); color: var(--muted); font-size: 12px; letter-spacing: 0.08em; text-transform: uppercase; border-top: none; }
.table tr:hover td { background: rgba(37, 99, 235, 0.05); }
.empty { color: var(--muted); padding: 10px 0; }
</style>
