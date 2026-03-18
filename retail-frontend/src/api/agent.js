import request from './request'

/** 店家侧智能助手：发送一条消息，返回助手回复与会话 ID（多轮续聊时传 conversation_id） */
export function agentChat(message, conversationId) {
  return request.post('/store/agent/chat', { message, conversation_id: conversationId || undefined })
}

/**
 * 流式输出：POST /store/agent/chat/stream，通过 onChunk 收片段、onDone(conversationId) 结束。
 * @param {string} message
 * @param {string} [conversationId]
 * @param {string} token - 登录 token
 * @param {{ onChunk: (data: string) => void, onDone: (conversationId: string | null) => void }} handlers
 */
export function agentChatStream(message, conversationId, token, { onChunk, onDone }) {
  return fetch('/api/store/agent/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
    body: JSON.stringify({ message, conversation_id: conversationId || undefined })
  }).then(async (res) => {
    if (!res.ok) {
      const t = await res.text()
      throw new Error(t || res.statusText || '请求失败')
    }
    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let lastEvent = null
    let doneConvId = null
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const parts = buffer.split('\n\n')
      buffer = parts.pop() || ''
      for (const block of parts) {
        let eventType = null
        for (const line of block.split('\n')) {
          if (line.startsWith('event: ')) eventType = line.slice(7).trim()
          else if (line.startsWith('data: ')) {
            const data = line.slice(6).trim()
            if (eventType === 'done') doneConvId = data || null
            else if (data) onChunk(data)
          }
        }
      }
    }
    if (buffer) {
      for (const line of buffer.split('\n')) {
        if (line.startsWith('event: ')) lastEvent = line.slice(7).trim()
        else if (line.startsWith('data: ') && lastEvent === 'done') doneConvId = line.slice(6).trim() || null
        else if (line.startsWith('data: ')) {
          const d = line.slice(6).trim()
          if (d) onChunk(d)
        }
      }
    }
    onDone(doneConvId)
  })
}
