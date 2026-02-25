import request from './request'

/** 店家侧智能助手：发送一条消息，返回助手回复 */
export function agentChat(message) {
  return request.post('/store/agent/chat', { message })
}
