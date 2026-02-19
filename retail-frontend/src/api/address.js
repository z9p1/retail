import request from './request'

export function listAddresses() {
  return request.get('/user/addresses')
}

export function addAddress(data) {
  return request.post('/user/addresses', data)
}

export function updateAddress(id, data) {
  return request.put(`/user/addresses/${id}`, data)
}

export function deleteAddress(id) {
  return request.delete(`/user/addresses/${id}`)
}
