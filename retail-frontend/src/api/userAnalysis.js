import request from './request'

export function listConsumers() {
  return request.get('/user-analysis/consumers')
}

export function searchUser(keyword) {
  return request.get('/user-analysis/search', { params: { keyword } })
}

export function getUserAnalysis(userId) {
  return request.get(`/user-analysis/${userId}`)
}
