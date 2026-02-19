<template>
  <div class="page">
    <div class="card">
      <h1>注册</h1>
      <div class="rules">
        <div class="rules-title">注册须知</div>
        <ul>
          <li><strong>账号</strong>：4-32 位，仅限字母、数字、下划线，且不能与已有账号重复</li>
          <li><strong>密码</strong>：8-20 位，须同时包含大写字母、小写字母、数字</li>
          <li><strong>昵称</strong>：选填，2-20 个字符</li>
          <li><strong>手机号</strong>：选填，须为 11 位有效手机号</li>
        </ul>
      </div>
      <form @submit.prevent="submit">
        <input v-model="form.username" placeholder="账号（必填）" maxlength="32" />
        <input v-model="form.password" type="password" placeholder="密码（必填）" maxlength="20" autocomplete="new-password" />
        <input v-model="form.nickname" placeholder="昵称（选填，2-20 字）" maxlength="20" />
        <input v-model="form.phone" placeholder="手机号（选填，11 位）" maxlength="11" />
        <select v-model="form.role">
          <option value="USER">用户</option>
          <option value="STORE">店家</option>
        </select>
        <button type="submit" :disabled="loading">{{ loading ? '注册中...' : '注册' }}</button>
        <p v-if="err" class="err">{{ err }}</p>
      </form>
      <router-link to="/login">去登录</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api/auth'

const router = useRouter()
const loading = ref(false)
const err = ref('')
const form = reactive({ username: '', password: '', nickname: '', phone: '', role: 'USER' })

const USERNAME_REG = /^[a-zA-Z0-9_]{4,32}$/
const PHONE_REG = /^1[3-9]\d{9}$/

function validate() {
  const u = (form.username || '').trim()
  if (!u) return '请填写账号'
  if (!USERNAME_REG.test(u)) return '账号须 4-32 位，仅限字母、数字、下划线'
  const p = form.password || ''
  if (!p) return '请填写密码'
  if (p.length < 8 || p.length > 20) return '密码须 8-20 位'
  if (!/[A-Z]/.test(p)) return '密码须包含至少一个大写字母'
  if (!/[a-z]/.test(p)) return '密码须包含至少一个小写字母'
  if (!/[0-9]/.test(p)) return '密码须包含至少一个数字'
  const nick = (form.nickname || '').trim()
  if (nick && (nick.length < 2 || nick.length > 20)) return '昵称须 2-20 个字符'
  const ph = (form.phone || '').trim()
  if (ph && !PHONE_REG.test(ph)) return '手机号须为 11 位有效号码'
  return null
}

async function submit() {
  err.value = ''
  const msg = validate()
  if (msg) {
    err.value = msg
    return
  }
  loading.value = true
  try {
    await register({
      username: form.username.trim(),
      password: form.password,
      nickname: form.nickname.trim() || undefined,
      phone: form.phone.trim() || undefined,
      role: form.role
    })
    router.push('/login')
  } catch (e) {
    err.value = e.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #0f0f23; }
.card { background: #1a1a2e; padding: 2rem; border-radius: 8px; width: 360px; }
.card h1 { margin: 0 0 1rem; color: #eee; font-size: 1.25rem; }
.rules { margin-bottom: 1.25rem; padding: 0.75rem 1rem; background: #16213e; border-radius: 6px; border: 1px solid #30363d; }
.rules-title { color: #8b949e; font-size: 0.85rem; margin-bottom: 0.5rem; }
.rules ul { margin: 0; padding-left: 1.2rem; color: #c9d1d9; font-size: 0.8rem; line-height: 1.6; }
.rules li { margin-bottom: 0.25rem; }
.card input, .card select { width: 100%; padding: 0.6rem; margin-bottom: 0.75rem; border: 1px solid #333; border-radius: 4px; background: #16213e; color: #eee; }
.card button { width: 100%; padding: 0.6rem; background: #e94560; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.err { color: #e94560; margin-top: 0.5rem; font-size: 0.9rem; }
.card a { color: #00d9ff; margin-top: 1rem; display: inline-block; }
</style>
