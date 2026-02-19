import request from './request'

export function getSimulatePurchase() {
  return request.get('/store/schedule/simulate-purchase')
}

export function setSimulatePurchaseEnabled(enabled) {
  return request.put('/store/schedule/simulate-purchase', { enabled })
}
