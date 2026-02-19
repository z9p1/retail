import request from './request'

export function getTraffic(range = '7') {
  return request.get('/traffic', { params: { range } })
}
