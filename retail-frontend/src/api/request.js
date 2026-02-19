import axios from 'axios'
import router from '../router'
import { useUserStore } from '../stores/user'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

request.interceptors.request.use(config => {
  const store = useUserStore()
  if (store.token) {
    config.headers.Authorization = `Bearer ${store.token}`
  }
  return config
})

request.interceptors.response.use(
  res => {
    const { code, data, message } = res.data
    if (code !== 0 && code !== undefined) {
      return Promise.reject(new Error(message || '请求失败'))
    }
    return data !== undefined ? data : res.data
  },
  err => {
    if (err.response?.status === 401) {
      const store = useUserStore()
      store.logout()
      const msg = err.response?.data?.message || ''
      const reason = msg.includes('别处登录') ? 'kicked' : 'timeout'
      router.push({ path: '/login', query: { reason } })
    }
    const msg = err.response?.data?.message || err.message || '网络错误'
    return Promise.reject(new Error(msg))
  }
)

export default request
