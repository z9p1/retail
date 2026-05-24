<template>
  <span class="ui-tag" :class="`ui-tag--${tone}`">
    <slot />
  </span>
</template>

<script setup>
defineProps({
  tone: { type: String, default: 'neutral' } // neutral | info | success | warning | danger
})
</script>

<style scoped>
.ui-tag {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: var(--font-12);
  border: 1px solid var(--border);
  background: var(--surface-2);
  color: var(--text-2);
  white-space: nowrap;
}
.ui-tag--info {
  border-color: rgba(37, 99, 235, 0.22);
  background: rgba(37, 99, 235, 0.10);
  color: var(--primary-2);
}
.ui-tag--success {
  border-color: rgba(22, 163, 74, 0.22);
  background: rgba(22, 163, 74, 0.10);
  color: #15803d;
}
.ui-tag--warning {
  border-color: rgba(245, 158, 11, 0.25);
  background: var(--warning-soft);
  color: #92400e;
}
.ui-tag--danger {
  border-color: rgba(239, 68, 68, 0.22);
  background: var(--danger-soft);
  color: #b91c1c;
}
</style>

