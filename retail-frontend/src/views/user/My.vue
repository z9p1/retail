<template>
  <div class="my">
    <h2>我的</h2>
    <p>昵称：{{ userStore.user?.nickname }}</p>
    <p>账号：{{ userStore.user?.username }}</p>
    <router-link to="/orders">我的订单</router-link>
  </div>
</template>

<script setup>
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
</script>

<style scoped>
.my h2 { margin: 0 0 1rem; }
.my a { color: #e94560; }
</style>
