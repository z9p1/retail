import { reactive } from 'vue'

const state = reactive({
  items: []
})

let seq = 1

export function useToast() {
  function push({ type = 'info', title = '', message = '', duration = 2600 } = {}) {
    const id = seq++
    state.items = [...state.items, { id, type, title, message }]
    window.setTimeout(() => {
      state.items = state.items.filter(x => x.id !== id)
    }, duration)
  }

  return {
    info: (message, title = '') => push({ type: 'info', message, title }),
    success: (message, title = '') => push({ type: 'success', message, title }),
    warning: (message, title = '') => push({ type: 'warning', message, title }),
    error: (message, title = '') => push({ type: 'error', message, title }),
    _state: state
  }
}

