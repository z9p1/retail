<template>
  <div class="mall">
    <h2>商城</h2>
    <div class="toolbar">
      <input v-model="name" placeholder="搜索商品" @keyup.enter="load" />
      <select v-model="orderBy" @change="load">
        <option value="">默认</option>
        <option value="price_asc">价格升序</option>
        <option value="price_desc">价格降序</option>
      </select>
      <button @click="load">查询</button>
    </div>
    <div class="grid">
      <div v-for="p in list" :key="p.id" class="card">
        <div class="name">{{ p.name }}</div>
        <div class="price">¥ {{ p.price }} 库存 {{ p.stock }}</div>
        <button @click="addCart(p)">加购物车</button>
        <button @click="buyNow(p)">立即购买</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listProducts } from '../../api/product'
import { useCartStore } from '../../stores/cart'
import { addCart as apiAddCart, clearCart as apiClearCart } from '../../api/cart'

const router = useRouter()
const cartStore = useCartStore()
const list = ref([])
const name = ref('')
const orderBy = ref('')

async function load() {
  try {
    list.value = await listProducts({ name: name.value || undefined, orderBy: orderBy.value || undefined })
  } catch (e) {
    console.error(e)
  }
}

async function addCart(p) {
  try {
    await apiAddCart(p.id, 1)
    cartStore.add(p, 1)
    alert('已加入购物车')
  } catch (e) {
    alert(e.message || '加入失败')
  }
}

async function buyNow(p) {
  try {
    await apiClearCart()
    await apiAddCart(p.id, 1)
    cartStore.clear()
    cartStore.add(p, 1)
    router.push('/cart?checkout=1')
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

onMounted(load)
</script>

<style scoped>
.mall h2 { margin: 0 0 1rem; }
.toolbar { margin-bottom: 1rem; display: flex; gap: 0.5rem; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 1rem; }
.card { background: #16213e; padding: 1rem; border-radius: 8px; }
.card .name { font-weight: bold; margin-bottom: 0.5rem; }
.card .price { color: #888; font-size: 0.9rem; margin-bottom: 0.5rem; }
.card button { margin-right: 0.5rem; margin-top: 0.5rem; }
</style>
