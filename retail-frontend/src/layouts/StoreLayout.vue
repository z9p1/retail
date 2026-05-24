<template>
  <div class="store-layout">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <div class="brand-mark">R</div>
        <div class="brand-text">
          <div class="brand-title">Retail Admin</div>
          <div class="brand-subtitle">Store Console</div>
        </div>
      </div>
      <nav class="menu">
        <div class="menu-group">概览</div>
        <router-link to="/store/workbench">工作台</router-link>
        <router-link to="/store/orders">订单管理</router-link>
        <router-link to="/store/products">商品管理</router-link>
        <div class="menu-group">运营</div>
        <router-link to="/store/online-users">用户监控</router-link>
        <router-link to="/store/user-analysis">用户分析</router-link>
        <router-link to="/store/schedule">任务管理</router-link>
        <div class="menu-group">系统</div>
        <router-link to="/store/settings">设置</router-link>
      </nav>
    </aside>
    <div class="body">
      <header class="header">
        <div class="header-left">
          <div class="header-title">店家控制台</div>
        </div>
        <div class="header-right">
          <div class="user-pill">
            <span class="avatar">{{ (userStore.user?.nickname || userStore.user?.username || 'S').slice(0,1) }}</span>
            <span class="user">{{ userStore.user?.nickname || userStore.user?.username }}</span>
          </div>
          <UiButton variant="ghost" @click="logout">退出</UiButton>
        </div>
      </header>
      <main class="main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'
import UiButton from '../components/ui/UiButton.vue'

const userStore = useUserStore()
const router = useRouter()

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.store-layout { display: flex; min-height: 100vh; background: var(--bg); }

.sidebar {
  width: 240px;
  flex-shrink: 0;
  background: #0b1220;
  color: rgba(255, 255, 255, 0.9);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
}
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 16px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-weight: 900;
  color: #fff;
  background: linear-gradient(180deg, rgba(37, 99, 235, 1), rgba(29, 78, 216, 1));
  box-shadow: 0 10px 26px rgba(37, 99, 235, 0.22);
}
.brand-title { font-weight: 900; letter-spacing: -0.02em; }
.brand-subtitle { margin-top: 1px; font-size: 12px; color: rgba(255,255,255,0.65); }

.menu { padding: 10px 10px 16px; }
.menu-group {
  margin: 14px 10px 8px;
  font-size: 12px;
  color: rgba(255,255,255,0.55);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}
.menu a {
  position: relative;
  display: flex;
  align-items: center;
  padding: 10px 12px;
  margin: 2px 0;
  border-radius: 10px;
  color: rgba(255,255,255,0.78);
  text-decoration: none;
}
.menu a:hover { background: rgba(255,255,255,0.06); color: #fff; }
.menu a.router-link-active {
  background: rgba(37, 99, 235, 0.22);
  color: #fff;
}
.menu a.router-link-active::before {
  content: "";
  position: absolute;
  left: -10px;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: 4px;
  background: rgba(37, 99, 235, 1);
}

.body { flex: 1; display: flex; flex-direction: column; min-width: 0; background: var(--bg); }
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  height: 56px;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
}
.header-title {
  font-weight: 900;
  color: var(--text);
  letter-spacing: -0.02em;
}
.header-right { display: flex; align-items: center; gap: 10px; }
.user-pill {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--surface-2);
}
.avatar {
  width: 26px;
  height: 26px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  font-weight: 900;
  color: rgba(37, 99, 235, 1);
  background: var(--primary-soft);
  border: 1px solid rgba(37, 99, 235, 0.18);
}
.user { color: var(--text-2); font-size: 14px; }

.main { flex: 1; padding: 20px; overflow: auto; }
</style>
