<template>
  <button
    class="ui-btn"
    :class="variantClass"
    :disabled="disabled || loading"
    type="button"
  >
    <span v-if="loading" class="ui-btn__spinner" aria-hidden="true"></span>
    <span class="ui-btn__content"><slot /></span>
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: { type: String, default: 'secondary' }, // primary | secondary | ghost | danger
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
})

const variantClass = computed(() => `ui-btn--${props.variant}`)
</script>

<style scoped>
.ui-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  height: var(--control-h);
  padding: 0 var(--space-4);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-2);
  background: var(--surface);
  color: var(--text);
  box-shadow: var(--shadow-1);
  cursor: pointer;
  user-select: none;
}
.ui-btn:hover:not(:disabled) {
  border-color: rgba(15, 23, 42, 0.22);
  transform: translateY(-0.5px);
}
.ui-btn:active:not(:disabled) {
  transform: translateY(0);
}
.ui-btn--primary {
  border-color: rgba(37, 99, 235, 0.25);
  background: linear-gradient(180deg, rgba(37, 99, 235, 0.96), rgba(29, 78, 216, 0.96));
  color: #fff;
}
.ui-btn--primary:hover:not(:disabled) {
  filter: brightness(1.03);
}
.ui-btn--ghost {
  background: transparent;
  box-shadow: none;
}
.ui-btn--danger {
  border-color: rgba(239, 68, 68, 0.22);
  background: rgba(239, 68, 68, 0.08);
  color: #b91c1c;
}
.ui-btn--danger:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.12);
}
.ui-btn__spinner {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.55);
  border-top-color: rgba(255, 255, 255, 0.95);
  animation: spin 0.8s linear infinite;
}
.ui-btn--secondary .ui-btn__spinner,
.ui-btn--ghost .ui-btn__spinner,
.ui-btn--danger .ui-btn__spinner {
  border-color: rgba(15, 23, 42, 0.25);
  border-top-color: rgba(15, 23, 42, 0.6);
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>

