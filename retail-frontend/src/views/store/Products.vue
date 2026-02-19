<template>
  <div class="products">
    <h2>商品管理</h2>
    <div class="toolbar">
      <input v-model="query.name" placeholder="名称" @keyup.enter="load" />
      <select v-model="query.status">
        <option value="">全部状态</option>
        <option value="ON_SALE">在售</option>
        <option value="OFF_SHELF">下架</option>
      </select>
      <button @click="load">查询</button>
      <button @click="showAdd = true">新增商品</button>
    </div>
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
          <td>{{ p.status === 'ON_SALE' ? '在售' : '下架' }}</td>
          <td>
            <button @click="toggleStatus(p)">{{ p.status === 'ON_SALE' ? '下架' : '上架' }}</button>
            <button @click="editStock(p)">改库存</button>
          </td>
        </tr>
      </tbody>
    </table>
    <div class="pagination">
      <button :disabled="page <= 1" @click="page--; load()">上一页</button>
      <span>第 {{ page }} 页</span>
      <button :disabled="list.length < 10" @click="page++; load()">下一页</button>
    </div>
    <div v-if="showAdd" class="modal">
      <div class="modal-content">
        <h3>新增商品</h3>
        <input v-model="addForm.name" placeholder="名称" />
        <input v-model.number="addForm.price" type="number" placeholder="价格" step="0.01" />
        <input v-model.number="addForm.stock" type="number" placeholder="库存" />
        <input v-model="addForm.description" placeholder="描述" />
        <div>
          <button @click="submitAdd">提交</button>
          <button @click="showAdd = false">取消</button>
        </div>
      </div>
    </div>
    <div v-if="editingStock" class="modal">
      <div class="modal-content">
        <h3>修改库存 - {{ editingStock.name }}</h3>
        <input v-model.number="stockVal" type="number" />
        <div>
          <button @click="submitStock">确定</button>
          <button @click="editingStock = null">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { storeListProducts, storeAddProduct, storeUpdateStatus, storeUpdateStock } from '../../api/product'

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
.products h2 { margin: 0 0 1rem; }
.toolbar { display: flex; gap: 0.5rem; margin-bottom: 1rem; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { border: 1px solid #333; padding: 0.5rem; text-align: left; }
.pagination { margin-top: 1rem; }
.modal { position: fixed; inset: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; }
.modal-content { background: #1a1a2e; padding: 1.5rem; border-radius: 8px; }
.modal-content input { display: block; width: 100%; margin-bottom: 0.5rem; padding: 0.5rem; }
.modal-content div { margin-top: 0.75rem; }
</style>
