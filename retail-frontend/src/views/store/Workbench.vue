<template>
  <div class="workbench">
    <h2>工作台</h2>
    <div class="workbench-layout">
      <div class="workbench-left">
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
      <aside class="workbench-right">
        <div class="traffic-panel">
          <h3>流量监控</h3>
          <div class="toolbar">
            <select v-model="trafficRange" @change="loadTraffic">
              <option value="today">今日</option>
              <option value="7">最近7天</option>
              <option value="30">最近30天</option>
            </select>
          </div>
          <div class="traffic-cards">
            <div class="tcard"><span class="label">UV</span><span class="value">{{ trafficData.uv ?? '-' }}</span></div>
            <div class="tcard"><span class="label">PV</span><span class="value">{{ trafficData.pv ?? '-' }}</span></div>
            <div class="tcard"><span class="label">下单笔数</span><span class="value">{{ trafficData.orderCount ?? '-' }}</span></div>
            <div class="tcard"><span class="label">下单人数</span><span class="value">{{ trafficData.userCount ?? '-' }}</span></div>
            <div class="tcard"><span class="label">成交金额</span><span class="value">{{ trafficData.amount ?? '-' }}</span></div>
          </div>
          <section class="trend-section">
            <h4>近 7 天销量趋势</h4>
            <div class="product-tabs">
              <button type="button" :class="{ active: selectedProductId === null }" @click="selectedProductId = null">全部</button>
              <button v-for="p in trendProducts" :key="p.productId" type="button" :class="{ active: selectedProductId === p.productId }" @click="selectedProductId = p.productId">{{ p.productName }}</button>
            </div>
            <div class="chart-wrap">
              <svg v-if="trend.dates && trend.dates.length" class="line-chart" viewBox="0 0 400 140" preserveAspectRatio="none">
                <defs>
                  <linearGradient id="workbenchLineGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="#00d9ff" stop-opacity="0.4" />
                    <stop offset="100%" stop-color="#00d9ff" stop-opacity="0" />
                  </linearGradient>
                </defs>
                <polyline fill="url(#workbenchLineGrad)" :points="areaChartPoints" stroke="none" />
                <polyline :points="lineChartPoints" fill="none" stroke="#00d9ff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" vector-effect="non-scaling-stroke" />
              </svg>
              <div v-if="trend.dates && trend.dates.length" class="chart-labels">
                <span v-for="d in trend.dates" :key="d" class="chart-label">{{ d.slice(5) }}</span>
              </div>
              <p v-else class="no-data">暂无数据</p>
            </div>
          </section>
        </div>
      </aside>
    </div>

    <button type="button" class="agent-fab" :class="{ open: agentOpen }" @click="agentOpen = !agentOpen" title="智能助手">智能助手</button>
    <div v-if="agentOpen" class="agent-panel" :class="{ 'agent-panel-shake': agentPanelShake }">
      <div class="agent-header">
        <span>智能助手</span>
        <button type="button" class="agent-close" @click="agentOpen = false">×</button>
      </div>
      <div class="agent-messages" ref="agentMessagesRef">
        <div v-if="!agentStore.messages.length" class="agent-hint">
          你可以问我库存相关，查询库存 / 当前库存，订单与发货，待发货有多少 / 有多少单没发，流量与销售，今日/最近 7 天/最近 30 天的 UV、PV、下单笔数、成交金额等。<br />
          退出登录后聊天内容将清空。
        </div>
        <div v-for="(m, i) in agentStore.messages" :key="i" :class="['agent-msg', m.role]">
          <span class="agent-msg-label">{{ m.role === 'user' ? '我' : '助手' }}</span>
          <span class="agent-msg-content">{{ m.content }}</span>
        </div>
        <div v-if="agentSending" class="agent-msg assistant agent-loading">
          <span class="agent-msg-label">助手</span>
          <span class="agent-msg-content agent-loading-dots"><span></span><span></span><span></span></span>
        </div>
      </div>
      <div class="agent-input-wrap">
        <input v-model="agentStore.input" type="text" placeholder="输入问题，最多20字" maxlength="20" @keyup.enter="sendAgentMessage" />
        <span class="agent-char-count">{{ (agentStore.input || '').length }}/20</span>
        <button type="button" class="agent-send" :disabled="agentSending || agentCooldown > 0" @click="sendAgentMessage">
          {{ agentSending ? '...' : agentCooldown > 0 ? `请稍后再试(${agentCooldown}s)` : '发送' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { getWorkbench } from '../../api/workbench'
import { getTraffic, getTrafficTrend } from '../../api/traffic'
import { agentChat } from '../../api/agent'
import { useAgentStore } from '../../stores/agent'

const agentStore = useAgentStore()
const data = ref({})
const agentOpen = ref(true)
const agentPanelShake = ref(false)
const agentSending = ref(false)
const agentCooldown = ref(0)
let agentCooldownTimer = null
const agentMessagesRef = ref(null)
const trafficRange = ref('7')
const trafficData = ref({})
const trend = ref({})
const selectedProductId = ref(null)

const zeroStockList = computed(() => {
  const list = data.value?.zeroStockProducts
  return Array.isArray(list) ? list : []
})

const trendProducts = computed(() => trend.value.products || [])
const currentAmounts = computed(() => {
  if (selectedProductId.value == null) return (trend.value.dailyTotalAmounts || []).map(Number)
  const p = trendProducts.value.find(x => x.productId === selectedProductId.value)
  return p ? (p.dailyAmounts || []).map(Number) : []
})
const maxAmount = computed(() => {
  const arr = currentAmounts.value
  return Math.max(1, ...arr)
})
const padding = { left: 20, right: 20, top: 10, bottom: 24 }
const chartW = 400 - padding.left - padding.right
const chartH = 140 - padding.top - padding.bottom
const lineChartPoints = computed(() => {
  const arr = currentAmounts.value
  const max = maxAmount.value
  if (!arr.length) return ''
  const step = arr.length <= 1 ? chartW : chartW / (arr.length - 1)
  return arr.map((v, i) => {
    const x = padding.left + i * step
    const y = padding.top + chartH - (v / max) * chartH
    return `${x},${y}`
  }).join(' ')
})
const areaChartPoints = computed(() => {
  const arr = currentAmounts.value
  const max = maxAmount.value
  if (!arr.length) return ''
  const step = arr.length <= 1 ? chartW : chartW / (arr.length - 1)
  const pts = arr.map((v, i) => {
    const x = padding.left + i * step
    const y = padding.top + chartH - (v / max) * chartH
    return `${x},${y}`
  })
  const bottomY = padding.top + chartH
  const firstX = padding.left
  const lastX = padding.left + (arr.length <= 1 ? 0 : chartW)
  return `${firstX},${bottomY} ${pts.join(' ')} ${lastX},${bottomY}`
})

onMounted(async () => {
  agentPanelShake.value = true
  setTimeout(() => { agentPanelShake.value = false }, 1500)
  try {
    data.value = await getWorkbench()
  } catch (e) {
    console.error(e)
  }
  loadTraffic()
  try {
    trend.value = await getTrafficTrend(7)
  } catch (e) {
    console.error(e)
  }
})

async function loadTraffic() {
  try {
    trafficData.value = await getTraffic(trafficRange.value)
  } catch (e) {
    console.error(e)
  }
}

async function sendAgentMessage() {
  const text = (agentStore.input || '').trim()
  if (!text || agentSending.value || agentCooldown.value > 0) return
  if (text.length > 20) {
    agentStore.addMessage('assistant', '问题最多20个字，请缩短后重试。')
    return
  }
  agentStore.addMessage('user', text)
  agentStore.clearInput()
  agentSending.value = true
  startAgentCooldown()
  nextTick(() => {
    if (agentMessagesRef.value) agentMessagesRef.value.scrollTop = agentMessagesRef.value.scrollHeight
  })
  try {
    const res = await agentChat(text)
    const reply = res?.reply ?? '暂无回复'
    agentStore.addMessage('assistant', reply)
  } catch (e) {
    const msg = e.message || '请稍后再试'
    agentStore.addMessage('assistant', (msg.includes('30秒') || msg.includes('一分钟')) ? msg : '请求失败：' + msg)
  } finally {
    agentSending.value = false
    nextTick(() => {
      if (agentMessagesRef.value) agentMessagesRef.value.scrollTop = agentMessagesRef.value.scrollHeight
    })
  }
}

function startAgentCooldown() {
  agentCooldown.value = 30
  if (agentCooldownTimer) clearInterval(agentCooldownTimer)
  agentCooldownTimer = setInterval(() => {
    agentCooldown.value--
    if (agentCooldown.value <= 0) clearInterval(agentCooldownTimer)
  }, 1000)
}
</script>

<style scoped>
.workbench h2 { margin: 0 0 1rem; }
.workbench-layout { display: flex; gap: 1.5rem; align-items: flex-start; }
.workbench-left { flex: 1; min-width: 0; }
.workbench-right { width: 360px; flex-shrink: 0; }

.cards { display: flex; gap: 1rem; flex-wrap: wrap; }
.card { background: #16213e; padding: 1.25rem; border-radius: 8px; min-width: 160px; }
.card .label { display: block; color: #888; font-size: 0.9rem; }
.card .value { font-size: 1.5rem; color: #00d9ff; }
.alert { padding: 1rem 1.25rem; border-radius: 8px; margin-bottom: 1rem; }
.alert-restock { background: #3d1a1a; border: 1px solid #e94560; color: #ffb3b3; }
.alert-restock ul { margin: 0.5rem 0 0 1.25rem; padding: 0; }
.alert-restock li { margin: 0.25rem 0; }

.highlights { margin-top: 2rem; padding: 1.25rem; background: #16213e; border-radius: 8px; }
.highlights h3 { margin: 0 0 0.75rem; font-size: 1rem; color: #00d9ff; }
.highlights ul { margin: 0; padding-left: 1.25rem; color: #bbb; font-size: 0.9rem; line-height: 1.7; }
.highlights li { margin: 0.35rem 0; }

.traffic-panel { background: #16213e; padding: 1.25rem; border-radius: 8px; position: sticky; top: 0; }
.traffic-panel h3 { margin: 0 0 0.75rem; font-size: 1rem; color: #00d9ff; }
.toolbar { margin-bottom: 0.75rem; }
.toolbar select { padding: 0.35rem 0.5rem; background: #1a1a2e; border: 1px solid #333; border-radius: 4px; color: #eee; }
.traffic-cards { display: flex; flex-wrap: wrap; gap: 0.5rem; margin-bottom: 1rem; }
.tcard { background: #1a1a2e; padding: 0.6rem 0.75rem; border-radius: 6px; min-width: 70px; }
.tcard .label { display: block; color: #888; font-size: 0.75rem; }
.tcard .value { font-size: 1rem; color: #00d9ff; }
.trend-section { margin-top: 1rem; }
.trend-section h4 { font-size: 0.9rem; margin: 0 0 0.5rem; color: #ccc; }
.product-tabs { margin-bottom: 0.5rem; }
.product-tabs button { margin-right: 0.35rem; margin-bottom: 0.35rem; padding: 0.25rem 0.5rem; border: 1px solid #333; border-radius: 4px; background: #1a1a2e; color: #ccc; cursor: pointer; font-size: 0.8rem; }
.product-tabs button.active { background: #00d9ff; color: #1a1a2e; border-color: #00d9ff; }
.chart-wrap { background: #1a1a2e; padding: 0.75rem; border-radius: 6px; min-height: 160px; }
.line-chart { width: 100%; height: 120px; display: block; }
.chart-labels { display: flex; justify-content: space-between; margin-top: 4px; padding: 0 2px; font-size: 0.65rem; color: #888; }
.no-data { color: #888; margin: 0; font-size: 0.85rem; }

.agent-fab { position: fixed; right: 24px; bottom: 24px; padding: 0.6rem 1rem; background: #00d9ff; color: #1a1a2e; border: none; border-radius: 8px; font-size: 0.9rem; cursor: pointer; box-shadow: 0 2px 8px rgba(0,217,255,0.4); z-index: 100; }
.agent-fab:hover { opacity: 0.9; }
.agent-panel { position: fixed; right: 24px; bottom: 70px; width: 360px; max-height: 420px; background: #0f2430; border: 1px solid #1e5f6f; border-radius: 8px; box-shadow: 0 4px 24px rgba(0,0,0,0.5), 0 0 0 1px rgba(0,217,255,0.15); z-index: 101; display: flex; flex-direction: column; }
.agent-panel-shake { animation: agent-panel-shake 0.5s ease-in-out 3; }
@keyframes agent-panel-shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-6px); }
  40% { transform: translateX(6px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(4px); }
}
.agent-header { display: flex; align-items: center; justify-content: space-between; padding: 0.75rem 1rem; border-bottom: 1px solid #1e5f6f; font-weight: 600; color: #00d9ff; background: rgba(0,217,255,0.06); }
.agent-close { background: none; border: none; color: #888; font-size: 1.2rem; cursor: pointer; padding: 0 0.25rem; }
.agent-close:hover { color: #eee; }
.agent-messages { flex: 1; overflow-y: auto; padding: 0.75rem; min-height: 200px; max-height: 280px; background: rgba(15,36,48,0.6); }
.agent-hint { color: #666; font-size: 0.8rem; line-height: 1.5; padding: 0.5rem 0; }
.agent-msg { margin-bottom: 0.75rem; }
.agent-msg-label { font-size: 0.75rem; color: #888; margin-right: 0.5rem; }
.agent-msg.user .agent-msg-content { color: #00d9ff; }
.agent-msg.assistant .agent-msg-content { color: #ccc; white-space: pre-wrap; word-break: break-word; }
.agent-loading-dots { display: inline-flex; gap: 4px; align-items: center; }
.agent-loading-dots span { width: 6px; height: 6px; border-radius: 50%; background: #00d9ff; animation: agent-loading-bounce 0.6s ease-in-out infinite both; }
.agent-loading-dots span:nth-child(2) { animation-delay: 0.15s; }
.agent-loading-dots span:nth-child(3) { animation-delay: 0.3s; }
@keyframes agent-loading-bounce { 0%, 80%, 100% { transform: scale(0.6); opacity: 0.6; } 40% { transform: scale(1); opacity: 1; } }
.agent-input-wrap { display: flex; flex-wrap: wrap; gap: 0.5rem; align-items: center; padding: 0.75rem; border-top: 1px solid #1e5f6f; }
.agent-input-wrap input { flex: 1; min-width: 120px; padding: 0.5rem; background: #132f3d; border: 1px solid #1e5f6f; border-radius: 4px; color: #eee; }
.agent-char-count { font-size: 0.75rem; color: #888; }
.agent-send { padding: 0.5rem 1rem; background: #00d9ff; color: #1a1a2e; border: none; border-radius: 4px; cursor: pointer; white-space: nowrap; }
.agent-send:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
