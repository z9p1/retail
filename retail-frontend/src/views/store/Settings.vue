<template>
  <div class="settings">
    <h2>设置</h2>
    <p>当前登录：{{ userStore.user?.nickname }} ({{ userStore.user?.username }})</p>
    <p>角色：{{ userStore.isStore ? '店家' : '用户' }}</p>
  </div>
</template>

<script setup>
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
</script>

<style scoped>
.settings h2 { margin: 0 0 1rem; }
</style>
