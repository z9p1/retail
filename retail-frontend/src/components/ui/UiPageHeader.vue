<template>
  <div class="ui-ph">
    <div class="ui-ph__left">
      <h2 class="ui-ph__title">{{ title }}</h2>
      <p v-if="description" class="ui-ph__desc">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="ui-ph__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' }
})
</script>

<style scoped>
.ui-ph {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}
.ui-ph__title {
  margin: 0;
  font-size: var(--font-20);
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--text);
}
.ui-ph__desc {
  margin: var(--space-1) 0 0;
  color: var(--muted);
  font-size: var(--font-14);
  line-height: 1.5;
}
.ui-ph__actions {
  display: flex;
  gap: var(--space-2);
  align-items: center;
}
</style>

