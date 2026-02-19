import request from './request'

export function getWorkbench() {
  return request.get('/store/workbench')
}
