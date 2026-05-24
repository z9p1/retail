<template>
  <div class="products">
    <UiPageHeader title="商品管理" description="管理商品上下架、库存与基础信息。">
      <template #actions>
        <UiButton variant="primary" @click="showAdd = true">新增商品</UiButton>
      </template>
    </UiPageHeader>

    <UiCard title="筛选条件" subtitle="按名称与状态过滤商品列表。">
      <div class="toolbar">
        <input v-model="query.name" placeholder="名称" @keyup.enter="load" />
        <select v-model="query.status">
          <option value="">全部状态</option>
          <option value="ON_SALE">在售</option>
          <option value="OFF_SHELF">下架</option>
        </select>
        <UiButton variant="secondary" @click="load">查询</UiButton>
      </div>
    </UiCard>

    <div class="spacer"></div>

    <UiCard title="商品列表" :padded="false">
      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>名称</th>
            <th>价格</th>
            <th>库存</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in list" :key="p.id">
            <td>{{ p.id }}</td>
            <td>{{ p.name }}</td>
            <td>{{ p.price }}</td>
            <td>{{ p.stock }}</td>
            <td>
              <UiTag :tone="p.status === 'ON_SALE' ? 'success' : 'neutral'">
                {{ p.status === 'ON_SALE' ? '在售' : '下架' }}
              </UiTag>
            </td>
            <td>
              <div class="row-actions">
                <UiButton variant="ghost" @click="toggleStatus(p)">{{ p.status === 'ON_SALE' ? '下架' : '上架' }}</UiButton>
                <UiButton variant="ghost" @click="editStock(p)">改库存</UiButton>
              </div>
            </td>
          </tr>
          <tr v-if="list.length === 0">
            <td colspan="6" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>
      <div class="pagination">
        <UiButton variant="ghost" :disabled="page <= 1" @click="page--; load()">上一页</UiButton>
        <span class="page-text">第 {{ page }} 页</span>
        <UiButton variant="ghost" :disabled="list.length < 10" @click="page++; load()">下一页</UiButton>
      </div>
    </UiCard>

    <UiModal v-if="showAdd" title="新增商品" @close="showAdd = false">
      <div class="form-grid">
        <label class="field">
          <span class="label">名称</span>
          <input v-model="addForm.name" placeholder="商品名称" />
        </label>
        <label class="field">
          <span class="label">价格</span>
          <input v-model.number="addForm.price" type="number" placeholder="价格" step="0.01" />
        </label>
        <label class="field">
          <span class="label">库存</span>
          <input v-model.number="addForm.stock" type="number" placeholder="库存" />
        </label>
        <label class="field full">
          <span class="label">描述</span>
          <input v-model="addForm.description" placeholder="简短描述" />
        </label>
      </div>
      <template #footer>
        <UiButton variant="ghost" @click="showAdd = false">取消</UiButton>
        <UiButton variant="primary" @click="submitAdd">提交</UiButton>
      </template>
    </UiModal>

    <UiModal v-if="editingStock" :title="`修改库存 - ${editingStock.name}`" width="460px" @close="editingStock = null">
      <label class="field">
        <span class="label">库存</span>
        <input v-model.number="stockVal" type="number" />
      </label>
      <template #footer>
        <UiButton variant="ghost" @click="editingStock = null">取消</UiButton>
        <UiButton variant="primary" @click="submitStock">确定</UiButton>
      </template>
    </UiModal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { storeListProducts, storeAddProduct, storeUpdateStatus, storeUpdateStock } from '../../api/product'
import UiPageHeader from '../../components/ui/UiPageHeader.vue'
import UiCard from '../../components/ui/UiCard.vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiModal from '../../components/ui/UiModal.vue'
import UiTag from '../../components/ui/UiTag.vue'

const list = ref([])
const page = ref(1)
const query = reactive({ name: '', status: '' })
const showAdd = ref(false)
const editingStock = ref(null)
const stockVal = ref(0)
const addForm = reactive({ name: '', price: '', stock: 0, description: '' })

async function load() {
  try {
    const res = await storeListProducts({ page: page.value, size: 10, ...query })
    list.value = res.records || []
  } catch (e) {
    console.error(e)
  }
}

function toggleStatus(p) {
  const newStatus = p.status === 'ON_SALE' ? 'OFF_SHELF' : 'ON_SALE'
  storeUpdateStatus(p.id, newStatus).then(() => load())
}

function editStock(p) {
  editingStock.value = p
  stockVal.value = p.stock
}

function submitStock() {
  if (editingStock.value == null) return
  storeUpdateStock(editingStock.value.id, stockVal.value).then(() => {
    editingStock.value = null
    load()
  })
}

function submitAdd() {
  storeAddProduct(addForm).then(() => {
    showAdd.value = false
    Object.assign(addForm, { name: '', price: '', stock: 0, description: '' })
    load()
  })
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
.spacer { height: var(--space-4); }

.table { width: 100%; border-collapse: collapse; }
.table th, .table td {
  border-top: 1px solid var(--border);
  padding: 12px 14px;
  text-align: left;
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
.empty { text-align: center; color: var(--muted); padding: 22px 14px; }

.row-actions { display: inline-flex; gap: 8px; }

.pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-2);
  padding: 12px 14px;
  border-top: 1px solid var(--border);
}
.page-text { color: var(--muted); font-size: 13px; }

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-3);
}
.field { display: grid; gap: 6px; }
.field.full { grid-column: 1 / -1; }
.label { font-size: var(--font-12); color: var(--muted); }
</style>
