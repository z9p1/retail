<template>
  <div class="workbench">
    <h2>工作台</h2>
    <div v-if="zeroStockList.length" class="alert alert-restock">
      <strong>补货提醒</strong>：以下商品库存为 0，请及时补货
      <ul>
        <li v-for="p in zeroStockList" :key="p.id">{{ p.name }}（ID: {{ p.id }}）</li>
      </ul>
    </div>
    <div class="cards">
      <div class="card">
        <span class="label">今日订单数</span>
        <span class="value">{{ data.todayOrderCount ?? '-' }}</span>
      </div>
      <div class="card">
        <span class="label">今日销售额</span>
        <span class="value">{{ data.todayAmount != null ? '¥' + Number(data.todayAmount).toFixed(2) : '-' }}</span>
      </div>
      <div class="card">
        <span class="label">本周订单数</span>
        <span class="value">{{ data.weekOrderCount ?? '-' }}</span>
      </div>
      <div class="card">
        <span class="label">本周销售额</span>
        <span class="value">{{ data.weekAmount != null ? '¥' + Number(data.weekAmount).toFixed(2) : '-' }}</span>
      </div>
      <div class="card">
        <span class="label">待发货订单</span>
        <span class="value">{{ data.pendingShipCount ?? '-' }}</span>
      </div>
      <div class="card">
        <span class="label">低库存商品(&lt;5)</span>
        <span class="value">{{ data.lowStockCount ?? '-' }}</span>
      </div>
    </div>
    <p class="tip">流量与销售数据请到「流量监控」查看</p>

    <section class="highlights">
      <h3>项目特色</h3>
      <ul>
        <li>Redis 在线用户监控：会话存 Redis，可查看在线用户、踢人下线、异地登录挤掉旧会话</li>
        <li>订单幂等（Redis）：下单支持 Idempotency-Key，5 分钟内相同 key 返回同一订单，防重复提交</li>
        <li>多处结果 Redis 缓存：流量统计、用户消费分析等结果缓存，减轻 DB 压力</li>
        <li>店家 / 用户双角色：店家工作台与用户端商城、购物车、订单分离</li>
        <li>定时任务模拟购物：整点执行模拟顾客下单，便于演示与压测</li>
        <li>工作台核心指标：待发货订单、低库存与零库存补货提醒</li>
        <li>技术栈：前端 Vue3 + Pinia + Vite，后端 Spring Boot + MyBatis-Plus + MySQL</li>
      </ul>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getWorkbench } from '../../api/workbench'

const data = ref({})

const zeroStockList = computed(() => {
  const list = data.value?.zeroStockProducts
  return Array.isArray(list) ? list : []
})

onMounted(async () => {
  try {
    data.value = await getWorkbench()
  } catch (e) {
    console.error(e)
  }
})
</script>

<style scoped>
.workbench h2 { margin: 0 0 1rem; }
.cards { display: flex; gap: 1rem; flex-wrap: wrap; }
.card { background: #16213e; padding: 1.25rem; border-radius: 8px; min-width: 160px; }
.card .label { display: block; color: #888; font-size: 0.9rem; }
.card .value { font-size: 1.5rem; color: #00d9ff; }
.tip { margin-top: 1.5rem; color: #666; }
.alert { padding: 1rem 1.25rem; border-radius: 8px; margin-bottom: 1rem; }
.alert-restock { background: #3d1a1a; border: 1px solid #e94560; color: #ffb3b3; }
.alert-restock ul { margin: 0.5rem 0 0 1.25rem; padding: 0; }
.alert-restock li { margin: 0.25rem 0; }

.highlights { margin-top: 2rem; padding: 1.25rem; background: #16213e; border-radius: 8px; }
.highlights h3 { margin: 0 0 0.75rem; font-size: 1rem; color: #00d9ff; }
.highlights ul { margin: 0; padding-left: 1.25rem; color: #bbb; font-size: 0.9rem; line-height: 1.7; }
.highlights li { margin: 0.35rem 0; }
</style>
