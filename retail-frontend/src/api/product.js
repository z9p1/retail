import request from './request'

/** 用户端：在售商品列表 */
export function listProducts(params) {
  return request.get('/user/products', { params })
}

export function getProduct(id) {
  return request.get(`/user/products/${id}`)
}

/** 店家端 */
export function storeListProducts(params) {
  return request.get('/store/products', { params })
}

export function storeAddProduct(data) {
  return request.post('/store/products', data)
}

export function storeUpdateStatus(id, status) {
  return request.put(`/store/products/${id}/status`, { status })
}

export function storeUpdateStock(id, stock) {
  return request.put(`/store/products/${id}/stock`, { stock })
}

export function storeLowStock(threshold = 5) {
  return request.get('/store/products/low-stock', { params: { threshold } })
}
