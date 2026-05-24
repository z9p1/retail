<template>
  <div class="ui-toastHost" aria-live="polite" aria-relevant="additions removals">
    <div v-for="t in toast._state.items" :key="t.id" class="ui-toast" :class="`ui-toast--${t.type}`">
      <div v-if="t.title" class="ui-toast__title">{{ t.title }}</div>
      <div class="ui-toast__msg">{{ t.message }}</div>
    </div>
  </div>
</template>

<script setup>
import { useToast } from './toast'
const toast = useToast()
</script>

<style scoped>
.ui-toastHost {
  position: fixed;
  right: 16px;
  top: 16px;
  z-index: 1200;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ui-toast {
  width: 320px;
  max-width: calc(100vw - 32px);
  background: var(--surface);
  border: 1px solid var(--border);
  border-left-width: 4px;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-2);
  padding: 12px 14px;
}
.ui-toast__title {
  font-weight: 800;
  margin-bottom: 2px;
  color: var(--text);
}
.ui-toast__msg {
  color: var(--text-2);
  line-height: 1.45;
}
.ui-toast--info { border-left-color: var(--primary); }
.ui-toast--success { border-left-color: var(--success); }
.ui-toast--warning { border-left-color: var(--warning); }
.ui-toast--error { border-left-color: var(--danger); }
</style>

