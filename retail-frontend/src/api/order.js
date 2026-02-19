import request from './request'

/**
 * 创建订单。shippingAddress 可选。idempotencyKey 同 key 在 5 分钟内返回同一订单。
 */
export function createOrder(items, idempotencyKey, shippingAddress) {
  const config = idempotencyKey ? { headers: { 'Idempotency-Key': idempotencyKey } } : {}
  return request.post('/user/orders', { items, shippingAddress: shippingAddress || undefined }, config)
}

export function payOrder(orderId) {
  return request.post(`/user/orders/${orderId}/pay`, {})
}

export function cancelOrder(orderId) {
  return request.post(`/user/orders/${orderId}/cancel`, {})
}

export function confirmOrder(orderId) {
  return request.post(`/user/orders/${orderId}/confirm`, {})
}

export function myOrders(params) {
  return request.get('/user/orders', { params })
}

export function orderDetail(orderId) {
  return request.get(`/user/orders/${orderId}`)
}

/** 店家 */
export function storeOrders(params) {
  return request.get('/store/orders', { params })
}

export function storeOrderDetail(orderId) {
  return request.get(`/store/orders/${orderId}`)
}

export function shipOrder(orderId) {
  return request.post(`/store/orders/${orderId}/ship`, {})
}

/** 店家：导出订单为 CSV（当前筛选条件，最多 5000 条） */
export function exportStoreOrders(params) {
  return request.get('/store/orders/export', { params, responseType: 'blob' })
}
