<template>
  <div class="settings">
    <h2>设置</h2>
    <p>当前登录：{{ userStore.user?.nickname }} ({{ userStore.user?.username }})</p>
    <p>角色：店家</p>
    <section class="form-section">
      <h3>修改密码</h3>
      <form @submit.prevent="submitPassword">
        <input v-model="pwd.old" type="password" placeholder="原密码" required />
        <input v-model="pwd.new" type="password" placeholder="新密码（8-20位，含大小写+数字）" required />
        <button type="submit" :disabled="pwdLoading">{{ pwdLoading ? '提交中...' : '修改密码' }}</button>
        <p v-if="pwdErr" class="err">{{ pwdErr }}</p>
      </form>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useUserStore } from '../../stores/user'
import { changePassword } from '../../api/auth'

const userStore = useUserStore()
const pwd = reactive({ old: '', new: '' })
const pwdLoading = ref(false)
const pwdErr = ref('')

async function submitPassword() {
  pwdErr.value = ''
  if (!pwd.old || !pwd.new) return
  pwdLoading.value = true
  try {
    await changePassword(pwd.old, pwd.new)
    alert('密码已修改')
    pwd.old = ''
    pwd.new = ''
  } catch (e) {
    pwdErr.value = e.message || '修改失败'
  } finally {
    pwdLoading.value = false
  }
}
</script>

<style scoped>
.settings h2 { margin: 0 0 1rem; }
.form-section { margin-top: 1.5rem; }
.form-section h3 { font-size: 1rem; margin-bottom: 0.75rem; }
.form-section input { width: 100%; max-width: 280px; padding: 0.5rem; margin-bottom: 0.5rem; display: block; }
.form-section button { padding: 0.5rem 1rem; margin-top: 0.5rem; }
.err { color: #e94560; font-size: 0.9rem; margin-top: 0.5rem; }
</style>
