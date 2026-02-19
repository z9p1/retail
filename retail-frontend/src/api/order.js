import request from './request'

/**
 * 创建订单。可选传 idempotencyKey，相同 key 在 5 分钟内重复请求返回同一订单，防重复提交。
 */
export function createOrder(items, idempotencyKey) {
  const config = idempotencyKey ? { headers: { 'Idempotency-Key': idempotencyKey } } : {}
  return request.post('/user/orders', items, config)
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
