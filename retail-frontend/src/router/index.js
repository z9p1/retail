import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { guest: true } },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue'), meta: { guest: true } },
  {
    path: '/store',
    component: () => import('../layouts/StoreLayout.vue'),
    meta: { requireStore: true },
    children: [
      { path: '', redirect: '/store/workbench' },
      { path: 'workbench', name: 'StoreWorkbench', component: () => import('../views/store/Workbench.vue') },
      { path: 'products', name: 'StoreProducts', component: () => import('../views/store/Products.vue') },
      { path: 'orders', name: 'StoreOrders', component: () => import('../views/store/Orders.vue') },
      { path: 'traffic', name: 'StoreTraffic', component: () => import('../views/store/Traffic.vue') },
      { path: 'user-analysis', name: 'StoreUserAnalysis', component: () => import('../views/store/UserAnalysis.vue') },
      { path: 'schedule', name: 'StoreSchedule', component: () => import('../views/store/Schedule.vue') },
      { path: 'settings', name: 'StoreSettings', component: () => import('../views/store/Settings.vue') }
    ]
  },
  {
    path: '/',
    component: () => import('../layouts/UserLayout.vue'),
    meta: { requireAuth: true },
    children: [
      { path: '', redirect: '/mall' },
      { path: 'mall', name: 'Mall', component: () => import('../views/user/Mall.vue') },
      { path: 'cart', name: 'Cart', component: () => import('../views/user/Cart.vue') },
      { path: 'orders', name: 'MyOrders', component: () => import('../views/user/Orders.vue') },
      { path: 'my', name: 'My', component: () => import('../views/user/My.vue') }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, _from, next) => {
  const store = useUserStore()
  if (to.meta.guest && store.isLoggedIn) {
    if (store.isStore) return next('/store')
    return next('/mall')
  }
  if (to.meta.requireStore && !store.isLoggedIn) return next('/login')
  if (to.meta.requireStore && !store.isStore) return next('/mall')
  if (to.meta.requireAuth && !store.isLoggedIn) return next('/login')
  next()
})

export default router
