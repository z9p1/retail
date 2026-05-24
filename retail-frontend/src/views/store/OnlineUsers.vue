<template>
  <div class="online-users">
    <UiPageHeader title="用户监控" description="展示 Redis 在线用户，可对异常会话进行下线处理。"/>

    <UiCard title="在线用户" :padded="false">
      <div class="hint">展示当前 Redis 在线用户，被踢下线的用户下次请求将跳转登录页。</div>
      <table class="table">
      <thead>
        <tr>
          <th>用户 ID</th>
          <th>账号</th>
          <th>昵称</th>
          <th>角色</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="u in list" :key="u.id">
          <td>{{ u.id }}</td>
          <td>{{ u.username }}</td>
          <td>{{ u.nickname || '—' }}</td>
          <td>{{ u.role === 'STORE' ? '店家' : '用户' }}</td>
          <td>
            <UiButton type="button" variant="danger" @click="kick(u.id)">踢下线</UiButton>
          </td>
        </tr>
        <tr v-if="list.length === 0 && !loading">
          <td colspan="5" class="empty">暂无在线用户</td>
        </tr>
      </tbody>
      </table>
      <div v-if="loading" class="loading">加载中...</div>
    </UiCard>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOnlineUsers, kickOnlineUser } from '../../api/onlineUsers'
import UiPageHeader from '../../components/ui/UiPageHeader.vue'
import UiCard from '../../components/ui/UiCard.vue'
import UiButton from '../../components/ui/UiButton.vue'
import { useToast } from '../../components/ui/toast'

const toast = useToast()

const list = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    list.value = await getOnlineUsers()
  } catch (e) {
    console.error(e)
    list.value = []
  } finally {
    loading.value = false
  }
}

async function kick(userId) {
  if (!confirm('确定将该用户踢下线吗？')) return
  try {
    await kickOnlineUser(userId)
    toast.success('已踢下线')
    await load()
  } catch (e) {
    toast.error(e.message || '操作失败')
  }
}

onMounted(load)
</script>

<style scoped>
.hint {
  padding: 12px 14px;
  color: var(--muted);
  font-size: 13px;
  border-bottom: 1px solid var(--border);
  background: var(--surface-2);
}
.table { width: 100%; border-collapse: collapse; }
.table th, .table td {
  border-top: 1px solid var(--border);
  padding: 12px 14px;
  text-align: left;
  color: var(--text-2);
}
.table th {
  background: var(--surface-2);
  color: var(--muted);
  font-size: var(--font-12);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  border-top: none;
}
.table tr:hover td { background: rgba(37, 99, 235, 0.05); }
.empty { text-align: center; color: var(--muted); padding: 22px 14px; }
.loading { padding: 12px 14px; color: var(--muted); border-top: 1px solid var(--border); }
</style>
