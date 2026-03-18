import request from './request'

/** 获取当前 Dify 应用选项与当前选中的 config key */
export function getDifyAppConfig() {
  return request.get('/store/agent-config/dify-app')
}

/** 设置当前使用的 Dify 应用（config key 名，如 11、test1） */
export function setDifyAppCurrent(app) {
  return request.put('/store/agent-config/dify-app', { app })
}
