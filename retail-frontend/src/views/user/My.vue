<template>
  <div class="my">
    <h2>我的</h2>
    <section class="profile">
      <p>账号：{{ userStore.user?.username }}（不可修改）</p>
      <form @submit.prevent="submitProfile" class="form-inline">
        <label>昵称 <input v-model="form.nickname" type="text" placeholder="昵称" maxlength="20" /></label>
        <label>手机 <input v-model="form.phone" type="text" placeholder="手机号（选填）" maxlength="11" /></label>
        <button type="submit" :disabled="profileLoading">{{ profileLoading ? '保存中...' : '保存' }}</button>
      </form>
      <p v-if="profileErr" class="err">{{ profileErr }}</p>
    </section>
    <section class="pwd-section">
      <h3>修改密码</h3>
      <form @submit.prevent="submitPassword">
        <input v-model="pwd.old" type="password" placeholder="原密码" />
        <input v-model="pwd.new" type="password" placeholder="新密码（8-20位，含大小写+数字）" />
        <button type="submit" :disabled="pwdLoading">{{ pwdLoading ? '提交中...' : '修改密码' }}</button>
        <p v-if="pwdErr" class="err">{{ pwdErr }}</p>
      </form>
    </section>
    <section class="address-section">
      <h3>收货地址（选填，下单时可选）</h3>
      <button type="button" class="btn-add" @click="openAddressForm()">新增地址</button>
      <ul v-if="addressList.length" class="address-list">
        <li v-for="a in addressList" :key="a.id" class="address-item">
          <span class="addr-text">{{ [a.receiver, a.phone, a.address].filter(Boolean).join(' ') || a.address }}</span>
          <button type="button" class="btn-sm" @click="openAddressForm(a)">编辑</button>
          <button type="button" class="btn-sm danger" @click="removeAddress(a.id)">删除</button>
        </li>
      </ul>
      <p v-else class="no-addr">暂无收货地址</p>
    </section>
    <div v-if="addressFormShow" class="modal">
      <div class="modal-content">
        <h4>{{ editingAddress ? '编辑地址' : '新增地址' }}</h4>
        <form @submit.prevent="saveAddress">
          <label>收货人 <input v-model="addrForm.receiver" type="text" placeholder="选填" /></label>
          <label>手机 <input v-model="addrForm.phone" type="text" placeholder="选填" maxlength="11" /></label>
          <label>详细地址 <input v-model="addrForm.address" type="text" placeholder="省市区街道门牌等" required /></label>
          <div class="modal-actions">
            <button type="submit" :disabled="addrSaving">{{ addrSaving ? '保存中...' : '保存' }}</button>
            <button type="button" @click="addressFormShow = false">取消</button>
          </div>
        </form>
        <p v-if="addrErr" class="err">{{ addrErr }}</p>
      </div>
    </div>
    <p><router-link to="/orders">我的订单</router-link></p>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '../../stores/user'
import { changePassword, updateProfile } from '../../api/auth'
import { listAddresses, addAddress, updateAddress, deleteAddress } from '../../api/address'

const userStore = useUserStore()
const form = reactive({ nickname: '', phone: '' })
const profileLoading = ref(false)
const profileErr = ref('')
const pwd = reactive({ old: '', new: '' })
const pwdLoading = ref(false)
const pwdErr = ref('')

const addressList = ref([])
const addressFormShow = ref(false)
const editingAddress = ref(null)
const addrForm = reactive({ receiver: '', phone: '', address: '' })
const addrSaving = ref(false)
const addrErr = ref('')

onMounted(() => {
  form.nickname = userStore.user?.nickname ?? ''
  form.phone = userStore.user?.phone ?? ''
  loadAddresses()
})

async function loadAddresses() {
  try {
    addressList.value = await listAddresses()
  } catch (e) {
    console.error(e)
  }
}

function openAddressForm(a) {
  editingAddress.value = a || null
  addrForm.receiver = a ? (a.receiver || '') : ''
  addrForm.phone = a ? (a.phone || '') : ''
  addrForm.address = a ? (a.address || '') : ''
  addrErr.value = ''
  addressFormShow.value = true
}

async function saveAddress() {
  addrErr.value = ''
  if (!addrForm.address || !addrForm.address.trim()) {
    addrErr.value = '请填写详细地址'
    return
  }
  addrSaving.value = true
  try {
    if (editingAddress.value) {
      await updateAddress(editingAddress.value.id, { ...addrForm })
      alert('已更新')
    } else {
      await addAddress(addrForm)
      alert('已添加')
    }
    addressFormShow.value = false
    loadAddresses()
  } catch (e) {
    addrErr.value = e.message || '保存失败'
  } finally {
    addrSaving.value = false
  }
}

async function removeAddress(id) {
  if (!confirm('确定删除该地址？')) return
  try {
    await deleteAddress(id)
    loadAddresses()
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

async function submitProfile() {
  profileErr.value = ''
  profileLoading.value = true
  try {
    await updateProfile(form.nickname, form.phone || undefined)
    userStore.user = { ...userStore.user, nickname: form.nickname, phone: form.phone }
    if (userStore.user) {
      localStorage.setItem('user', JSON.stringify(userStore.user))
    }
    alert('已保存')
  } catch (e) {
    profileErr.value = e.message || '保存失败'
  } finally {
    profileLoading.value = false
  }
}

async function submitPassword() {
  pwdErr.value = ''
  if (!pwd.old || !pwd.new) return
  pwdLoading.value = true
  try {
    await changePassword(pwd.old, pwd.new)
    alert('密码已修改')
    pwd.old = ''
    pwd.new = ''
  } catch (e) {
    pwdErr.value = e.message || '修改失败'
  } finally {
    pwdLoading.value = false
  }
}
</script>

<style scoped>
.my h2 { margin: 0 0 1rem; }
.profile, .pwd-section { margin-bottom: 1.5rem; }
.pwd-section h3 { font-size: 1rem; margin-bottom: 0.5rem; }
.form-inline label { display: block; margin-bottom: 0.5rem; }
.form-inline input { padding: 0.4rem; margin-left: 0.5rem; width: 160px; }
.form-inline button, .pwd-section button { margin-top: 0.5rem; padding: 0.4rem 0.8rem; }
.pwd-section input { display: block; margin-bottom: 0.5rem; padding: 0.4rem; width: 200px; }
.address-section { margin-bottom: 1.5rem; }
.address-section h3 { font-size: 1rem; margin-bottom: 0.5rem; }
.btn-add { padding: 0.4rem 0.8rem; margin-bottom: 0.75rem; }
.address-list { list-style: none; padding: 0; margin: 0; }
.address-item { padding: 0.5rem 0; border-bottom: 1px solid #333; display: flex; align-items: center; gap: 0.5rem; }
.addr-text { flex: 1; font-size: 0.9rem; }
.btn-sm { padding: 0.2rem 0.5rem; font-size: 0.85rem; }
.btn-sm.danger { color: #e94560; }
.no-addr { color: #888; margin: 0; }
.modal { position: fixed; inset: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 10; }
.modal-content { background: #1a1a2e; padding: 1.25rem; border-radius: 8px; min-width: 280px; }
.modal-content h4 { margin: 0 0 1rem; }
.modal-content label { display: block; margin-bottom: 0.5rem; }
.modal-content label input { width: 100%; padding: 0.4rem; margin-top: 0.25rem; }
.modal-actions { margin-top: 1rem; display: flex; gap: 0.5rem; }
.err { color: #e94560; font-size: 0.9rem; margin-top: 0.5rem; }
.my a { color: #e94560; }
</style>
