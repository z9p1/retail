<template>
  <div class="orders">
    <UiPageHeader title="订单管理" description="筛选订单、发货与导出。">
      <template #actions>
        <UiButton variant="secondary" @click="exportCsv">导出 CSV</UiButton>
      </template>
    </UiPageHeader>

    <UiCard title="筛选条件" subtitle="支持按状态、用户关键字与日期范围筛选。">
      <div class="toolbar">
        <input v-model="query.userKeyword" type="text" placeholder="用户昵称/账号" class="search-input" />
        <select v-model="query.status">
          <option value="">全部</option>
          <option value="PENDING_SHIP">待发货</option>
          <option value="SHIPPED">已发货</option>
          <option value="COMPLETED">已完成</option>
          <option value="CANCELLED">已取消</option>
        </select>
        <div class="date-wrap">
          <span class="date-range">下单日期</span>
          <input v-model="query.startDate" type="date" class="date-input" />
          <span class="to">至</span>
          <input v-model="query.endDate" type="date" class="date-input" />
        </div>
        <UiButton variant="primary" @click="load">查询</UiButton>
      </div>
    </UiCard>

    <div class="spacer"></div>

    <UiCard title="订单列表" :padded="false">
      <table class="table">
      <thead>
        <tr>
          <th>订单号</th>
          <th>用户</th>
          <th>商品名称</th>
          <th>商品数量</th>
          <th>金额</th>
          <th>收货地址</th>
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
          <td>{{ o.shippingAddress || '—' }}</td>
          <td>
            <UiTag :tone="statusTone(o.status)">{{ statusText(o.status) }}</UiTag>
          </td>
          <td>{{ o.createTime }}</td>
          <td>
            <div class="row-actions">
              <UiButton v-if="o.status === 'PENDING_SHIP'" variant="primary" @click="ship(o.id)">发货</UiButton>
              <UiButton variant="ghost" @click="viewDetail(o.id)">详情</UiButton>
            </div>
          </td>
        </tr>
        <tr v-if="list.length === 0">
          <td colspan="9" class="empty">暂无数据</td>
        </tr>
      </tbody>
      </table>
      <div class="pagination">
        <UiButton variant="ghost" :disabled="page <= 1" @click="page--; load()">上一页</UiButton>
        <span class="page-text">第 {{ page }} / {{ totalPages }} 页</span>
        <UiButton variant="ghost" :disabled="page >= totalPages" @click="page++; load()">下一页</UiButton>
      </div>
    </UiCard>

    <UiModal v-if="detail" title="订单详情" width="640px" @close="detail = null">
      <div class="detail-meta">
        <div class="meta-row">
          <span class="k">订单号</span><span class="v">{{ detail.order?.orderNo }}</span>
          <span class="k">金额</span><span class="v">¥ {{ detail.order?.totalAmount }}</span>
          <span class="k">状态</span>
          <span class="v"><UiTag :tone="statusTone(detail.order?.status)">{{ statusText(detail.order?.status) }}</UiTag></span>
        </div>
        <div v-if="detail.order?.shippingAddress" class="meta-row">
          <span class="k">收货地址</span><span class="v">{{ detail.order.shippingAddress }}</span>
        </div>
      </div>
      <div class="items-title">商品明细</div>
      <table class="table table--inner" v-if="detail.items && detail.items.length">
        <thead><tr><th>商品</th><th>数量</th><th>小计</th></tr></thead>
        <tbody>
          <tr v-for="i in detail.items" :key="i.id">
            <td>{{ i.productName }}</td>
            <td>{{ i.quantity }}</td>
            <td>¥ {{ i.subtotal }}</td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty inner-empty">暂无明细</div>
      <template #footer>
        <UiButton variant="primary" @click="detail = null">关闭</UiButton>
      </template>
    </UiModal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { storeOrders, storeOrderDetail, shipOrder, exportStoreOrders } from '../../api/order'
import UiPageHeader from '../../components/ui/UiPageHeader.vue'
import UiCard from '../../components/ui/UiCard.vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiModal from '../../components/ui/UiModal.vue'
import UiTag from '../../components/ui/UiTag.vue'
import { useToast } from '../../components/ui/toast'

const toast = useToast()

const list = ref([])
const page = ref(1)
const pageSize = 10
const totalPages = ref(1)
const query = reactive({ status: '', userKeyword: '', startDate: '', endDate: '' })
const detail = ref(null)

function statusText(s) {
  const m = { PENDING_PAY: '待支付', CANCELLED: '已取消', PENDING_SHIP: '待发货', SHIPPED: '已发货', COMPLETED: '已完成' }
  return m[s] || s
}

function statusTone(s) {
  const m = { PENDING_PAY: 'warning', CANCELLED: 'danger', PENDING_SHIP: 'warning', SHIPPED: 'info', COMPLETED: 'success' }
  return m[s] || 'neutral'
}

async function load() {
  try {
    const params = { page: page.value, size: pageSize, status: query.status || undefined }
    if (query.userKeyword && query.userKeyword.trim()) params.userKeyword = query.userKeyword.trim()
    if (query.startDate && query.startDate.trim()) params.startDate = query.startDate.trim()
    if (query.endDate && query.endDate.trim()) params.endDate = query.endDate.trim()
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
  shipOrder(id).then(() => {
    toast.success('已标记为发货')
    load()
  }).catch(e => toast.error(e.message || '操作失败'))
}

function viewDetail(id) {
  storeOrderDetail(id).then(d => { detail.value = d })
}

async function exportCsv() {
  try {
    const params = {}
    if (query.status) params.status = query.status
    if (query.userKeyword && query.userKeyword.trim()) params.userKeyword = query.userKeyword.trim()
    if (query.startDate && query.startDate.trim()) params.startDate = query.startDate.trim()
    if (query.endDate && query.endDate.trim()) params.endDate = query.endDate.trim()
    const blob = await exportStoreOrders(params)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'orders.csv'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    console.error(e)
    toast.error(e.message || '导出失败')
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
  align-items: center;
}
.date-wrap {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}
.date-range { color: var(--muted); font-size: 13px; }
.to { color: var(--muted); }
.spacer { height: var(--space-4); }

.table { width: 100%; border-collapse: collapse; }
.table th, .table td {
  border-top: 1px solid var(--border);
  padding: 12px 14px;
  color: var(--text-2);
}
.table th {
  font-size: var(--font-12);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--muted);
  background: var(--surface-2);
  border-top: none;
}
.table tr:hover td { background: rgba(37, 99, 235, 0.05); }
.table--inner th, .table--inner td { padding: 10px 12px; }

.row-actions { display: inline-flex; gap: 8px; align-items: center; }
.empty { text-align: center; color: var(--muted); padding: 22px 14px; }
.inner-empty { padding: 12px 0; }

.pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-2);
  padding: 12px 14px;
  border-top: 1px solid var(--border);
}
.page-text { color: var(--muted); font-size: 13px; }

.detail-meta {
  border: 1px solid var(--border);
  background: var(--surface-2);
  border-radius: var(--radius-md);
  padding: var(--space-4);
}
.meta-row {
  display: grid;
  grid-template-columns: 80px 1fr 60px 1fr 60px 1fr;
  gap: 10px 12px;
  align-items: center;
  margin-bottom: 10px;
}
.meta-row:last-child { margin-bottom: 0; grid-template-columns: 80px 1fr; }
.k { color: var(--muted); font-size: 12px; }
.v { color: var(--text-2); font-size: 13px; }
.items-title { margin: var(--space-4) 0 var(--space-2); font-weight: 800; color: var(--text); }
</style>
