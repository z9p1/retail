import request from './request'

export function getTraffic(range = '7') {
  return request.get('/traffic', { params: { range } })
}

/** 销量趋势：近 7 天每日总金额 + 各商品每日金额 */
export function getTrafficTrend(days = 7) {
  return request.get('/traffic/trend', { params: { days } })
}
