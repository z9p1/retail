<template>
  <div class="orders">
    <h2>订单管理</h2>
    <div class="toolbar">
      <input v-model="query.userKeyword" type="text" placeholder="用户昵称/账号" class="search-input" />
      <select v-model="query.status">
        <option value="">全部</option>
        <option value="PENDING_SHIP">待发货</option>
        <option value="SHIPPED">已发货</option>
        <option value="COMPLETED">已完成</option>
        <option value="CANCELLED">已取消</option>
      </select>
      <button @click="load">查询</button>
    </div>
    <table class="table">
      <thead>
        <tr>
          <th>订单号</th>
          <th>用户</th>
          <th>商品名称</th>
          <th>商品数量</th>
          <th>金额</th>
          <th>状态</th>
          <th>下单时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="o in list" :key="o.id">
          <td>{{ o.orderNo }}</td>
          <td>{{ o.userDisplayName ?? o.userId }}</td>
          <td>{{ o.productSummary ?? '—' }}</td>
          <td>{{ o.totalQuantity ?? '—' }}</td>
          <td>{{ o.totalAmount }}</td>
          <td>{{ statusText(o.status) }}</td>
          <td>{{ o.createTime }}</td>
          <td>
            <button v-if="o.status === 'PENDING_SHIP'" @click="ship(o.id)">发货</button>
            <button @click="viewDetail(o.id)">详情</button>
          </td>
        </tr>
      </tbody>
    </table>
    <div class="pagination">
      <button :disabled="page <= 1" @click="page--; load()">上一页</button>
      <span>第 {{ page }} / {{ totalPages }} 页</span>
      <button :disabled="page >= totalPages" @click="page++; load()">下一页</button>
    </div>
    <div v-if="detail" class="modal">
      <div class="modal-content">
        <h3>订单详情</h3>
        <p>订单号: {{ detail.order?.orderNo }} 金额: {{ detail.order?.totalAmount }} 状态: {{ statusText(detail.order?.status) }}</p>
        <ul>
          <li v-for="i in detail.items" :key="i.id">{{ i.productName }} x {{ i.quantity }} = {{ i.subtotal }}</li>
        </ul>
        <button @click="detail = null">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { storeOrders, storeOrderDetail, shipOrder } from '../../api/order'

const list = ref([])
const page = ref(1)
const pageSize = 10
const totalPages = ref(1)
const query = reactive({ status: '', userKeyword: '' })
const detail = ref(null)

function statusText(s) {
  const m = { PENDING_PAY: '待支付', CANCELLED: '已取消', PENDING_SHIP: '待发货', SHIPPED: '已发货', COMPLETED: '已完成' }
  return m[s] || s
}

async function load() {
  try {
    const params = { page: page.value, size: pageSize, status: query.status || undefined }
    if (query.userKeyword && query.userKeyword.trim()) params.userKeyword = query.userKeyword.trim()
    const res = await storeOrders(params)
    list.value = res.records || []
    const total = res.total ?? 0
    const fromTotal = total > 0 ? Math.ceil(total / pageSize) : 0
    totalPages.value = (res.pages != null && res.pages > 0) ? res.pages : Math.max(1, fromTotal)
  } catch (e) {
    console.error(e)
  }
}

function ship(id) {
  shipOrder(id).then(() => load())
}

function viewDetail(id) {
  storeOrderDetail(id).then(d => { detail.value = d })
}

onMounted(load)
</script>

<style scoped>
.orders h2 { margin: 0 0 1rem; }
.toolbar { margin-bottom: 1rem; display: flex; gap: 0.5rem; align-items: center; }
.toolbar .search-input { padding: 0.35rem 0.5rem; min-width: 140px; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { border: 1px solid #333; padding: 0.5rem; }
.pagination { margin-top: 1rem; }
.modal { position: fixed; inset: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; }
.modal-content { background: #1a1a2e; padding: 1.5rem; border-radius: 8px; max-width: 400px; }
</style>
