<template>
  <div class="login-page">
    <header class="top-bar">
      <span class="top-bar-placeholder"></span>
      <button type="button" class="btn-intro" @click="showIntro = true">项目简介</button>
    </header>
    <div class="card">
      <h1>店家线上零售系统(ai实现)</h1>
      <form @submit.prevent="submit">
        <input v-model="form.username" placeholder="账号" required />
        <input v-model="form.password" type="password" placeholder="密码" required />
        <button type="submit" :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
        <p v-if="err" class="err">{{ err }}</p>
      </form>
      <p class="tip">首次使用请先注册</p>
      <router-link to="/register">注册账号</router-link>
    </div>
    <div v-if="alertMsg" class="modal-overlay" @click.self="alertMsg = ''">
      <div class="modal alert-modal">
        <p class="alert-text">{{ alertMsg }}</p>
        <button type="button" class="modal-close" @click="alertMsg = ''">确定</button>
      </div>
    </div>
    <div v-if="showIntro" class="modal-overlay" @click.self="closeIntro">
      <div class="modal">
        <h3>项目简介</h3>
        <div class="modal-body">
          <p>由柏楠制作的 AI 项目，由于春节期间时间仓促，做得比较简陋。核心目的是展示当前 AI 强大的编程能力，全程没写过代码，此项目由 Cursor 制作。</p>
          <p><strong>实现功能</strong></p>
          <ol>
            <li>店家与顾客的正常交互</li>
            <li>通过定时任务模拟顾客购物</li>
            <li>一些常见的购物业务场景</li>
            <li>项目使用 Vue3 + Spring + Redis + MyBatis + docker 的技术选型，没有用到 MQ</li>
          </ol>
        </div>
        <button type="button" class="modal-close" @click="closeIntro">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { login } from '../api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const err = ref('')
const showIntro = ref(false)
const alertMsg = ref('')

const INTRO_SEEN_KEY = 'retail_project_intro_seen'

onMounted(() => {
  const reason = route.query.reason
  if (reason === 'kicked') {
    alertMsg.value = '您的账号已在别处登录，请重新登录。'
    router.replace({ path: '/login' })
  } else if (reason === 'timeout') {
    alertMsg.value = '由于长时间未操作，登录已超时，请重新登录。'
    router.replace({ path: '/login' })
  }
  if (!localStorage.getItem(INTRO_SEEN_KEY)) showIntro.value = true
})

function closeIntro() {
  showIntro.value = false
  localStorage.setItem(INTRO_SEEN_KEY, '1')
}

const form = reactive({ username: 'store', password: 'admin123' })

async function submit() {
  err.value = ''
  loading.value = true
  try {
    const data = await login(form.username, form.password)
    userStore.setLogin(data)
    if (data.role === 'STORE') router.push('/store')
    else router.push('/mall')
  } catch (e) {
    err.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { min-height: 100vh; display: flex; flex-direction: column; align-items: center; background: #0f0f23; padding-top: 1rem; }
.top-bar { position: absolute; top: 0; left: 0; right: 0; height: 52px; display: flex; align-items: center; justify-content: flex-end; padding: 0 1.5rem; }
.top-bar-placeholder { flex: 1; }
.btn-intro { padding: 0.4rem 0.8rem; background: #1a1a2e; color: #00d9ff; border: 1px solid #333; border-radius: 4px; cursor: pointer; font-size: 0.9rem; }
.btn-intro:hover { background: #16213e; border-color: #00d9ff; }
.card { background: #1a1a2e; padding: 2rem; border-radius: 8px; width: 320px; margin: auto 0; }
.card h1 { margin: 0 0 1.5rem; color: #eee; font-size: 1.25rem; }
.card input, .card select { width: 100%; padding: 0.6rem; margin-bottom: 0.75rem; border: 1px solid #333; border-radius: 4px; background: #16213e; color: #eee; }
.card button { width: 100%; padding: 0.6rem; background: #e94560; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.card button:disabled { opacity: 0.6; }
.err { color: #e94560; font-size: 0.9rem; margin-top: 0.5rem; }
.tip { margin-top: 1rem; color: #888; font-size: 0.85rem; }
.card a { color: #00d9ff; margin-top: 0.5rem; display: inline-block; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: #1a1a2e; padding: 1.5rem; border-radius: 8px; max-width: 420px; width: 90%; max-height: 85vh; overflow-y: auto; border: 1px solid #333; }
.modal h3 { margin: 0 0 1rem; color: #eee; font-size: 1.1rem; }
.modal-body { color: #ccc; font-size: 0.95rem; line-height: 1.6; }
.modal-body p { margin: 0 0 0.75rem; }
.modal-body ol { margin: 0.5rem 0 1rem 1.25rem; padding: 0; }
.modal-body li { margin-bottom: 0.35rem; }
.modal-close { margin-top: 1rem; padding: 0.5rem 1.2rem; background: #e94560; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.modal-close:hover { opacity: 0.9; }
.alert-modal { max-width: 340px; }
.alert-text { color: #eee; margin: 0 0 1rem; font-size: 1rem; }
</style>
