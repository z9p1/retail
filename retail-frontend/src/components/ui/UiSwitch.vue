<template>
  <label class="ui-switch">
    <input class="ui-switch__input" type="checkbox" :checked="modelValue" @change="onChange" />
    <span class="ui-switch__track" aria-hidden="true"></span>
  </label>
</template>

<script setup>
defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

function onChange(e) {
  emit('update:modelValue', e.target.checked)
}
</script>

<style scoped>
.ui-switch {
  position: relative;
  display: inline-flex;
  align-items: center;
  width: 46px;
  height: 26px;
}
.ui-switch__input {
  opacity: 0;
  width: 0;
  height: 0;
}
.ui-switch__track {
  position: absolute;
  inset: 0;
  border-radius: 999px;
  background: rgba(100, 116, 139, 0.35);
  border: 1px solid var(--border);
  transition: 0.2s ease;
}
.ui-switch__track::before {
  content: "";
  position: absolute;
  height: 20px;
  width: 20px;
  left: 3px;
  top: 50%;
  transform: translateY(-50%);
  border-radius: 999px;
  background: var(--surface);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-1);
  transition: 0.2s ease;
}
.ui-switch__input:checked + .ui-switch__track {
  background: rgba(37, 99, 235, 0.28);
  border-color: rgba(37, 99, 235, 0.35);
}
.ui-switch__input:checked + .ui-switch__track::before {
  transform: translate(20px, -50%);
  border-color: rgba(37, 99, 235, 0.35);
}
</style>

