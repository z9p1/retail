<template>
  <div class="user-layout">
    <aside class="sidebar">
      <div class="sidebar-title">用户中心</div>
      <nav class="menu">
        <router-link to="/mall">商城</router-link>
        <router-link to="/cart">购物车</router-link>
        <router-link to="/orders">订单</router-link>
        <router-link to="/my">我的</router-link>
      </nav>
    </aside>
    <div class="body">
      <header class="header">
        <span class="logo">店家线上零售系统</span>
        <span class="user">{{ userStore.user?.nickname }}</span>
        <button class="btn-logout" @click="logout">退出</button>
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

const userStore = useUserStore()
const router = useRouter()

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.user-layout { display: flex; min-height: 100vh; }
.sidebar { width: 200px; flex-shrink: 0; background: #001529; color: #fff; }
.sidebar-title { padding: 16px; font-weight: 600; color: rgba(255,255,255,0.9); border-bottom: 1px solid rgba(255,255,255,0.1); }
.menu { padding: 12px 0; }
.menu a { display: block; padding: 10px 16px; color: rgba(255,255,255,0.75); text-decoration: none; }
.menu a:hover { color: #fff; background: rgba(255,255,255,0.08); }
.menu a.router-link-active { color: #e94560; background: rgba(233,69,96,0.15); }
.body { flex: 1; display: flex; flex-direction: column; min-width: 0; background: #f0f2f5; }
.header { display: flex; align-items: center; padding: 0 24px; height: 48px; background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }
.logo { font-weight: 600; color: #1a1a2e; }
.user { margin-left: auto; margin-right: 16px; color: #666; font-size: 14px; }
.btn-logout { padding: 4px 12px; font-size: 14px; }
.main { flex: 1; padding: 24px; overflow: auto; }
</style>
