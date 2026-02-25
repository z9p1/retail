import { defineStore } from 'pinia'
import { ref } from 'vue'

/** 店家智能助手状态：切换工作台/商品/订单等模块后再切回来仍保留输入框内容和对话记录 */
export const useAgentStore = defineStore('agent', () => {
  const input = ref('')
  const messages = ref([])

  function addMessage(role, content) {
    messages.value = [...messages.value, { role, content }]
  }

  function clearInput() {
    input.value = ''
  }

  /** 用户退出登录时调用，清空输入与聊天记录 */
  function clearAll() {
    input.value = ''
    messages.value = []
  }

  return { input, messages, addMessage, clearInput, clearAll }
})
