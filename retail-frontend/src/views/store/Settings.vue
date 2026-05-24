<template>
  <div class="settings">
    <UiPageHeader title="设置" description="账号信息与安全设置。"/>

    <UiCard title="账号信息" subtitle="当前登录的店家账号。">
      <div class="kv">
        <div class="k">昵称</div>
        <div class="v">{{ userStore.user?.nickname || '—' }}</div>
        <div class="k">账号</div>
        <div class="v">{{ userStore.user?.username || '—' }}</div>
        <div class="k">角色</div>
        <div class="v"><UiTag tone="info">店家</UiTag></div>
      </div>
    </UiCard>

    <div class="spacer"></div>

    <UiCard title="修改密码" subtitle="建议定期更新密码以提升安全性。">
      <form class="form" @submit.prevent="submitPassword">
        <label class="field">
          <span class="label">原密码</span>
          <input v-model="pwd.old" type="password" placeholder="原密码" required />
        </label>
        <label class="field">
          <span class="label">新密码</span>
          <input v-model="pwd.new" type="password" placeholder="新密码（8-20位，含大小写+数字）" required />
        </label>
        <div class="actions">
          <UiButton variant="primary" :disabled="pwdLoading">{{ pwdLoading ? '提交中...' : '修改密码' }}</UiButton>
        </div>
        <p v-if="pwdErr" class="err">{{ pwdErr }}</p>
      </form>
    </UiCard>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useUserStore } from '../../stores/user'
import { changePassword } from '../../api/auth'
import UiPageHeader from '../../components/ui/UiPageHeader.vue'
import UiCard from '../../components/ui/UiCard.vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiTag from '../../components/ui/UiTag.vue'
import { useToast } from '../../components/ui/toast'

const userStore = useUserStore()
const pwd = reactive({ old: '', new: '' })
const pwdLoading = ref(false)
const pwdErr = ref('')
const toast = useToast()

async function submitPassword() {
  pwdErr.value = ''
  if (!pwd.old || !pwd.new) return
  pwdLoading.value = true
  try {
    await changePassword(pwd.old, pwd.new)
    toast.success('密码已修改')
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
.kv {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 10px 12px;
  align-items: center;
}
.k { color: var(--muted); font-size: 12px; }
.v { color: var(--text-2); font-size: 14px; }
.spacer { height: var(--space-4); }

.form {
  display: grid;
  gap: var(--space-3);
  max-width: 420px;
}
.field { display: grid; gap: 6px; }
.label { font-size: var(--font-12); color: var(--muted); }
.actions { margin-top: var(--space-2); }
.err { color: #e94560; font-size: 0.9rem; margin-top: 0.5rem; }
</style>
