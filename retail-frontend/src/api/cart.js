import request from './request'

/** 获取当前用户购物车列表（持久化） */
export function getCart() {
  return request.get('/user/cart')
}

/** 加入购物车，quantity 默认 1 */
export function addCart(productId, quantity = 1) {
  return request.post('/user/cart', { productId, quantity })
}

/** 修改数量（覆盖） */
export function updateCartQty(productId, quantity) {
  return request.put('/user/cart', { productId, quantity })
}

/** 删除一项 */
export function removeCartItem(productId) {
  return request.delete(`/user/cart/item/${productId}`)
}

/** 清空购物车 */
export function clearCart() {
  return request.delete('/user/cart')
}
