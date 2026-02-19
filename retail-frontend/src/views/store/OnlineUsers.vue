<template>
  <div class="online-users">
    <h2>用户监控</h2>
    <p class="tip">展示当前 Redis 在线用户，被踢下线的用户下次请求将跳转登录页。</p>
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
            <button type="button" class="btn-kick" @click="kick(u.id)">踢下线</button>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-if="list.length === 0 && !loading" class="empty">暂无在线用户</p>
    <p v-if="loading" class="loading">加载中...</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOnlineUsers, kickOnlineUser } from '../../api/onlineUsers'

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
    await load()
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

onMounted(load)
</script>

<style scoped>
.online-users h2 { margin: 0 0 0.5rem; }
.tip { color: #666; font-size: 0.9rem; margin-bottom: 1rem; }
.table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 2px rgba(0,0,0,0.06); }
.table th, .table td { border: 1px solid #eee; padding: 10px 12px; text-align: left; }
.table th { background: #fafafa; color: #333; font-weight: 600; }
.btn-kick { padding: 4px 10px; font-size: 13px; background: #e94560; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.btn-kick:hover { opacity: 0.9; }
.empty, .loading { color: #888; margin-top: 1rem; }
</style>
