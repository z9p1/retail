<template>
  <div class="ui-modal__overlay" role="dialog" aria-modal="true" @click.self="onOverlay">
    <div class="ui-modal" :style="{ width }">
      <header class="ui-modal__header">
        <div class="ui-modal__title">{{ title }}</div>
        <button class="ui-modal__close" type="button" @click="$emit('close')" aria-label="Close">×</button>
      </header>
      <div class="ui-modal__body">
        <slot />
      </div>
      <footer v-if="$slots.footer" class="ui-modal__footer">
        <slot name="footer" />
      </footer>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  width: { type: String, default: '520px' },
  closeOnOverlay: { type: Boolean, default: true }
})
const emit = defineEmits(['close'])

function onOverlay() {
  if (props.closeOnOverlay) {
    emit('close')
  }
}

function onKeydown(e) {
  if (e.key === 'Escape') emit('close')
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<style scoped>
.ui-modal__overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-5);
  z-index: 1000;
}
.ui-modal {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-2);
  max-width: 92vw;
}
.ui-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--border);
}
.ui-modal__title {
  font-weight: 800;
  color: var(--text);
}
.ui-modal__close {
  height: 28px;
  width: 28px;
  padding: 0;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--surface-2);
  cursor: pointer;
  box-shadow: none;
}
.ui-modal__body {
  padding: var(--space-5);
}
.ui-modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
  padding: var(--space-4) var(--space-5);
  border-top: 1px solid var(--border);
}
</style>

