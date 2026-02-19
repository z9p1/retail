<template>
  <div class="cart">
    <h2>购物车</h2>
    <div v-if="cartLoading" class="loading">加载中...</div>
    <div v-else-if="!cartStore.items.length" class="empty">购物车为空</div>
    <table v-else class="table">
      <thead>
        <tr>
          <th>商品</th>
          <th>单价</th>
          <th>数量</th>
          <th>小计</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="i in cartStore.items" :key="i.productId">
          <td>{{ i.productName }}</td>
          <td>{{ i.price }}</td>
          <td>
            <input type="number" v-model.number="i.quantity" min="1" @change="onQtyChange(i, i.quantity)" />
          </td>
          <td>{{ (i.price * i.quantity).toFixed(2) }}</td>
          <td><button @click="onRemove(i.productId)">删除</button></td>
        </tr>
      </tbody>
    </table>
    <div v-if="cartStore.items.length" class="footer">
      <div class="address-row">
        <label class="address-label">收货地址（选填）</label>
        <select v-model="addressChoice" class="address-select">
          <option value="">不填</option>
          <option v-for="a in addressList" :key="a.id" :value="'saved:' + a.id">{{ [a.receiver, a.phone, a.address].filter(Boolean).join(' ') || a.address }}</option>
          <option value="manual">其他（手动输入）</option>
        </select>
        <input v-if="addressChoice === 'manual'" v-model="shippingAddress" type="text" placeholder="省市区街道门牌等" class="address-input" />
      </div>
      <span>合计: {{ cartStore.totalAmount.toFixed(2) }}</span>
      <button @click="checkout" :disabled="checkoutLoading">{{ checkoutLoading ? '提交中...' : '去结算' }}</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart'
import { createOrder, payOrder } from '../../api/order'
import { listAddresses } from '../../api/address'
import { getCart, updateCartQty, removeCartItem, clearCart } from '../../api/cart'

const router = useRouter()
const cartStore = useCartStore()
const addressList = ref([])
const addressChoice = ref('')
const shippingAddress = ref('')
const cartLoading = ref(false)

onMounted(async () => {
  cartLoading.value = true
  try {
    const [addrs, list] = await Promise.all([listAddresses(), getCart()])
    addressList.value = addrs
    cartStore.setItems(list || [])
  } catch (e) {
    console.error(e)
  } finally {
    cartLoading.value = false
  }
})

async function onQtyChange(item, newQty) {
  if (newQty <= 0) {
    try {
      await removeCartItem(item.productId)
      cartStore.remove(item.productId)
    } catch (e) {
      alert(e.message || '操作失败')
    }
    return
  }
  try {
    await updateCartQty(item.productId, newQty)
    cartStore.updateQty(item.productId, newQty)
  } catch (e) {
    alert(e.message || '更新失败')
  }
}

async function onRemove(productId) {
  try {
    await removeCartItem(productId)
    cartStore.remove(productId)
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

watch(addressChoice, (v) => {
  if (v && v.startsWith('saved:')) {
    const id = Number(v.slice(6))
    const a = addressList.value.find(x => x.id === id)
    if (a) shippingAddress.value = [a.receiver, a.phone, a.address].filter(Boolean).join(' ') || a.address
  } else if (v !== 'manual') {
    shippingAddress.value = ''
  }
})

const checkoutLoading = ref(false)
function getShippingAddressForSubmit() {
  if (addressChoice.value === 'manual') return shippingAddress.value || undefined
  if (addressChoice.value && addressChoice.value.startsWith('saved:')) {
    const id = Number(addressChoice.value.slice(6))
    const a = addressList.value.find(x => x.id === id)
    return a ? ([a.receiver, a.phone, a.address].filter(Boolean).join(' ') || a.address) : undefined
  }
  return undefined
}
async function checkout() {
  const items = cartStore.toOrderItems()
  if (!items.length) return
  if (checkoutLoading.value) return
  checkoutLoading.value = true
  const idempotencyKey = typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : ''
  try {
    const order = await createOrder(items, idempotencyKey, getShippingAddressForSubmit())
    await payOrder(order.id)
    await clearCart()
    cartStore.clear()
    alert('支付成功')
    router.push('/orders')
  } catch (e) {
    alert(e.message || '下单失败')
  } finally {
    checkoutLoading.value = false
  }
}
</script>

<style scoped>
.cart h2 { margin: 0 0 1rem; }
.loading, .empty { color: #888; margin: 0.5rem 0; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { border: 1px solid #333; padding: 0.5rem; }
.footer { margin-top: 1rem; }
.address-row { margin-bottom: 0.5rem; }
.address-label { display: block; margin-bottom: 0.25rem; font-size: 0.9rem; }
.address-select { padding: 0.4rem; min-width: 200px; margin-right: 0.5rem; }
.address-input { width: 100%; max-width: 400px; padding: 0.4rem; margin-top: 0.5rem; }
.footer button { margin-left: 1rem; }
</style>
