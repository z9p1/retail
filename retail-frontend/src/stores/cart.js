import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/** 服务端返回的项与本地一致：productId, productName, price, quantity, imageUrl */
function toItem(row) {
  return {
    productId: row.productId,
    productName: row.productName,
    price: row.price != null ? Number(row.price) : 0,
    quantity: row.quantity ?? 0,
    imageUrl: row.imageUrl
  }
}

export const useCartStore = defineStore('cart', () => {
  const items = ref([]) // { productId, productName, price, quantity, imageUrl }

  const totalCount = computed(() => items.value.reduce((s, i) => s + i.quantity, 0))
  const totalAmount = computed(() => items.value.reduce((s, i) => s + i.price * i.quantity, 0))

  /** 仅本地加一项（与后端同步由调用方请求 API 后调用） */
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

  /** 用服务端列表覆盖本地（进入购物车页时用） */
  function setItems(rows) {
    items.value = (rows || []).map(toItem)
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

  return { items, totalCount, totalAmount, add, setItems, updateQty, remove, clear, toOrderItems }
})
