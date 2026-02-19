<template>
  <div class="cart">
    <h2>购物车</h2>
    <div v-if="!cartStore.items.length">购物车为空</div>
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
            <input type="number" v-model.number="i.quantity" min="1" @change="cartStore.updateQty(i.productId, i.quantity)" />
          </td>
          <td>{{ (i.price * i.quantity).toFixed(2) }}</td>
          <td><button @click="cartStore.remove(i.productId)">删除</button></td>
        </tr>
      </tbody>
    </table>
    <div v-if="cartStore.items.length" class="footer">
      <span>合计: {{ cartStore.totalAmount.toFixed(2) }}</span>
      <button @click="checkout" :disabled="checkoutLoading">{{ checkoutLoading ? '提交中...' : '去结算' }}</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart'
import { createOrder, payOrder } from '../../api/order'

const router = useRouter()
const cartStore = useCartStore()

const checkoutLoading = ref(false)
async function checkout() {
  const items = cartStore.toOrderItems()
  if (!items.length) return
  if (checkoutLoading.value) return
  checkoutLoading.value = true
  const idempotencyKey = typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : ''
  try {
    const order = await createOrder(items, idempotencyKey)
    await payOrder(order.id)
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
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { border: 1px solid #333; padding: 0.5rem; }
.footer { margin-top: 1rem; }
.footer button { margin-left: 1rem; }
</style>
