<template>
  <div class="user-analysis">
    <h2>用户消费分析</h2>
    <div class="consumers-bar">
      <span class="bar-label">全部消费者：</span>
      <span
        v-for="u in consumers"
        :key="u.id"
        class="consumer-name"
        @click="fillAndSearch(u)"
      >{{ u.nickname || u.username }}</span>
      <span v-if="!consumers.length" class="bar-empty">暂无消费者</span>
    </div>
    <div class="toolbar">
      <input ref="searchInputRef" v-model="keyword" placeholder="用户ID/手机/昵称" @keyup.enter="search" />
      <button @click="search">搜索</button>
    </div>
    <div v-if="userList.length" class="user-list">
      <p>选择用户查看分析：</p>
      <button v-for="u in userList" :key="u.id" @click="loadAnalysis(u.id)">{{ u.nickname || u.username }} (ID:{{ u.id }})</button>
    </div>
    <div v-if="analysis" class="result">
      <h3>消费汇总</h3>
      <p>订单数: {{ analysis.orderCount }} 总金额: {{ analysis.totalAmount }}</p>
      <p>首次支付: {{ analysis.firstPayTime }} 最近支付: {{ analysis.lastPayTime }}</p>
      <h4>订单明细列表</h4>
      <table class="table" v-if="analysis.orders && analysis.orders.length">
        <thead><tr><th>订单号</th><th>金额</th><th>状态</th><th>下单/支付时间</th></tr></thead>
        <tbody>
          <tr v-for="o in analysis.orders" :key="o.id">
            <td>{{ o.orderNo }}</td>
            <td>¥ {{ o.totalAmount }}</td>
            <td>{{ o.status }}</td>
            <td>{{ o.createTime }} / {{ o.payTime }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else>暂无订单</p>
      <h4>商品偏好</h4>
      <table class="table">
        <thead><tr><th>商品</th><th>数量</th><th>金额</th></tr></thead>
        <tbody>
          <tr v-for="(pref, i) in analysis.preference" :key="i">
            <td>{{ pref.productName }}</td>
            <td>{{ pref.quantity }}</td>
            <td>{{ pref.amount }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listConsumers, searchUser, getUserAnalysis } from '../../api/userAnalysis'

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
  }
}

async function loadAnalysis(userId) {
  try {
    analysis.value = await getUserAnalysis(userId)
  } catch (e) {
    console.error(e)
  }
}

onMounted(loadConsumers)
</script>

<style scoped>
.user-analysis { background: #0d1117; min-height: 100%; padding: 1rem; color: #e6edf3; }
.user-analysis h2 { margin: 0 0 1rem; color: #e6edf3; }
.consumers-bar { margin-bottom: 1rem; padding: 0.75rem; background: #161b22; border: 1px solid #30363d; border-radius: 8px; }
.bar-label { color: #8b949e; margin-right: 0.5rem; }
.consumer-name { display: inline-block; margin-right: 0.75rem; margin-bottom: 0.25rem; color: #58a6ff; cursor: pointer; text-decoration: underline; }
.consumer-name:hover { color: #79c0ff; }
.bar-empty { color: #6e7681; }
.toolbar { margin-bottom: 1rem; }
.toolbar input { background: #161b22; border: 1px solid #30363d; color: #e6edf3; }
.user-list { margin-bottom: 1rem; color: #e6edf3; }
.user-list button { margin-right: 0.5rem; margin-bottom: 0.5rem; }
.result { background: #161b22; border: 1px solid #30363d; padding: 1rem; border-radius: 8px; color: #e6edf3; }
.result h3, .result h4 { color: #e6edf3; }
.result p { color: #c9d1d9; }
.table { width: 100%; border-collapse: collapse; margin-top: 0.5rem; color: #e6edf3; }
.table th, .table td { border: 1px solid #30363d; padding: 0.4rem; background: #0d1117; color: #e6edf3; }
.table th { background: #161b22; color: #8b949e; }
</style>
