import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useCartStore = defineStore('cart', () => {
  const items = ref([]) // { productId, productName, price, quantity, imageUrl }

  const totalCount = computed(() => items.value.reduce((s, i) => s + i.quantity, 0))
  const totalAmount = computed(() => items.value.reduce((s, i) => s + i.price * i.quantity, 0))

  function add(product, quantity = 1) {
    const exist = items.value.find(i => i.productId === product.id)
    if (exist) exist.quantity += quantity
    else items.value.push({
      productId: product.id,
      productName: product.name,
      price: product.price,
      quantity,
      imageUrl: product.imageUrl
    })
  }

  function updateQty(productId, quantity) {
    const item = items.value.find(i => i.productId === productId)
    if (item) {
      if (quantity <= 0) remove(productId)
      else item.quantity = quantity
    }
  }

  function remove(productId) {
    items.value = items.value.filter(i => i.productId !== productId)
  }

  function clear() {
    items.value = []
  }

  function toOrderItems() {
    return items.value.map(i => ({ productId: i.productId, quantity: i.quantity }))
  }

  return { items, totalCount, totalAmount, add, updateQty, remove, clear, toOrderItems }
})
