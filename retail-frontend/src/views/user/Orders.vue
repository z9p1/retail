<template>
  <div class="orders">
    <h2>我的订单</h2>
    <div class="tabs">
      <button :class="{ active: status === '' }" @click="status = ''; load()">全部</button>
      <button :class="{ active: status === 'PENDING_PAY' }" @click="status = 'PENDING_PAY'; load()">待支付</button>
      <button :class="{ active: status === 'PENDING_SHIP' }" @click="status = 'PENDING_SHIP'; load()">待发货</button>
      <button :class="{ active: status === 'SHIPPED' }" @click="status = 'SHIPPED'; load()">已发货</button>
      <button :class="{ active: status === 'COMPLETED' }" @click="status = 'COMPLETED'; load()">已完成</button>
      <button :class="{ active: status === 'CANCELLED' }" @click="status = 'CANCELLED'; load()">已取消</button>
    </div>
    <table class="table">
      <thead>
        <tr>
          <th>订单号</th>
          <th>金额</th>
          <th>状态</th>
          <th>下单时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="o in list" :key="o.id">
          <td>{{ o.orderNo }}</td>
          <td>{{ o.totalAmount }}</td>
          <td>{{ statusText(o.status) }}</td>
          <td>{{ o.createTime }}</td>
          <td>
            <button v-if="o.status === 'PENDING_PAY'" @click="pay(o.id)">去支付</button>
            <button v-if="o.status === 'PENDING_PAY'" @click="cancel(o.id)">取消</button>
            <button v-if="o.status === 'SHIPPED'" @click="confirm(o.id)">确认收货</button>
            <button @click="viewDetail(o.id)">详情</button>
          </td>
        </tr>
      </tbody>
    </table>
    <div class="pagination">
      <button :disabled="page <= 1" @click="page--; load()">上一页</button>
      <span>第 {{ page }} 页</span>
      <button :disabled="list.length < 10" @click="page++; load()">下一页</button>
    </div>
    <div v-if="detail" class="modal">
      <div class="modal-content">
        <h3>订单详情</h3>
        <p>订单号: {{ detail.order?.orderNo }} 金额: {{ detail.order?.totalAmount }}</p>
        <ul>
          <li v-for="i in detail.items" :key="i.id">{{ i.productName }} x {{ i.quantity }} = {{ i.subtotal }}</li>
        </ul>
        <button @click="detail = null">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { myOrders, orderDetail, payOrder, cancelOrder, confirmOrder } from '../../api/order'

const list = ref([])
const page = ref(1)
const status = ref('')
const detail = ref(null)

function statusText(s) {
  const m = { PENDING_PAY: '待支付', CANCELLED: '已取消', PENDING_SHIP: '待发货', SHIPPED: '已发货', COMPLETED: '已完成' }
  return m[s] || s
}

async function load() {
  try {
    const res = await myOrders({ page: page.value, size: 10, status: status.value || undefined })
    list.value = res.records || []
  } catch (e) {
    console.error(e)
  }
}

function pay(id) {
  payOrder(id).then(() => load())
}

function cancel(id) {
  cancelOrder(id).then(() => load())
}

function confirm(id) {
  confirmOrder(id).then(() => load())
}

function viewDetail(id) {
  orderDetail(id).then(d => { detail.value = d })
}

onMounted(load)
</script>

<style scoped>
.orders h2 { margin: 0 0 1rem; }
.tabs { margin-bottom: 1rem; }
.tabs button { margin-right: 0.5rem; }
.tabs button.active { color: #e94560; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { border: 1px solid #333; padding: 0.5rem; }
.pagination { margin-top: 1rem; }
.modal { position: fixed; inset: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; }
.modal-content { background: #1a1a2e; padding: 1.5rem; border-radius: 8px; }
</style>
