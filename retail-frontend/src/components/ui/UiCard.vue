<template>
  <section class="ui-card" :class="{ padded }">
    <header v-if="title || $slots.header || $slots.actions" class="ui-card__header">
      <div class="ui-card__headerLeft">
        <slot name="header">
          <div v-if="title" class="ui-card__title">{{ title }}</div>
          <div v-if="subtitle" class="ui-card__subtitle">{{ subtitle }}</div>
        </slot>
      </div>
      <div v-if="$slots.actions" class="ui-card__actions">
        <slot name="actions" />
      </div>
    </header>
    <div class="ui-card__body">
      <slot />
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  padded: { type: Boolean, default: true }
})
</script>

<style scoped>
.ui-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-1);
}
.ui-card.padded .ui-card__body {
  padding: var(--space-5);
}
.ui-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-5);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--border);
  background: linear-gradient(180deg, var(--surface-2), var(--surface));
  border-top-left-radius: var(--radius-md);
  border-top-right-radius: var(--radius-md);
}
.ui-card__title {
  font-weight: 700;
  color: var(--text);
  letter-spacing: -0.01em;
}
.ui-card__subtitle {
  margin-top: var(--space-1);
  font-size: var(--font-12);
  color: var(--muted);
}
.ui-card__actions {
  display: flex;
  gap: var(--space-2);
  align-items: center;
}
.ui-card__body {
  padding: var(--space-5);
}
</style>

