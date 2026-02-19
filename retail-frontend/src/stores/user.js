import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const role = computed(() => user.value?.role || '')
  const isStore = computed(() => role.value === 'STORE')
  const isLoggedIn = computed(() => !!token.value)

  function setLogin(data) {
    token.value = data.token
    user.value = {
      userId: data.userId,
      username: data.username,
      role: data.role,
      nickname: data.nickname
    }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, role, isStore, isLoggedIn, setLogin, logout }
})
