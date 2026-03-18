import { defineStore } from 'pinia'
import { ref } from 'vue'

/** 店家智能助手状态：切换工作台/商品/订单等模块后再切回来仍保留输入框内容和对话记录；conversationId 用于多轮续聊（Java 侧持久化） */
export const useAgentStore = defineStore('agent', () => {
  const input = ref('查询最近 30 天的 UV')
  const messages = ref([])
  const conversationId = ref('')

  function addMessage(role, content) {
    messages.value = [...messages.value, { role, content }]
  }

  /** 流式输出：追加上一条助手消息内容（上一条须为 assistant） */
  function appendToLastMessage(chunk) {
    const list = messages.value
    if (list.length && list[list.length - 1].role === 'assistant') {
      messages.value = [...list.slice(0, -1), { ...list[list.length - 1], content: list[list.length - 1].content + chunk }]
    }
  }

  /** 将上一条助手消息内容替换为 content（流式出错时用） */
  function setLastMessageContent(content) {
    const list = messages.value
    if (list.length && list[list.length - 1].role === 'assistant') {
      messages.value = [...list.slice(0, -1), { ...list[list.length - 1], content: content || '' }]
    }
  }

  function clearInput() {
    input.value = ''
  }

  /** 设置当前会话 ID（后端返回后保存，下次请求带上以续聊） */
  function setConversationId(id) {
    conversationId.value = id || ''
  }

  /** 用户退出登录时调用，清空输入与聊天记录 */
  function clearAll() {
    input.value = ''
    messages.value = []
    conversationId.value = ''
  }

  return { input, messages, conversationId, addMessage, appendToLastMessage, setLastMessageContent, clearInput, setConversationId, clearAll }
})
