<script setup>
import { computed, onMounted, ref } from 'vue'
import { BarChart3, CalendarDays, CheckCircle2, Clock3, Gauge } from '@lucide/vue'
import { adminApi } from '../api'
import ChartPanel from '../components/ChartPanel.vue'

const loading = ref(false)
const stats = ref({
  totalBookings: 0,
  pendingBookings: 0,
  approvedBookings: 0,
  todayBookings: 0,
  approvalRate: 0,
  roomUsage: [],
  statusDistribution: [],
  bookingTrend: [],
})

const roomUsageOption = computed(() => ({
  grid: { left: 84, right: 24, top: 18, bottom: 30 },
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  xAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#eef1f5' } },
    axisLabel: { color: '#667085' },
  },
  yAxis: {
    type: 'category',
    data: stats.value.roomUsage.map((item) => item.label).reverse(),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#344054' },
  },
  series: [
    {
      type: 'bar',
      data: stats.value.roomUsage.map((item) => item.value).reverse(),
      barWidth: 14,
      itemStyle: {
        color: '#2563eb',
        borderRadius: [0, 4, 4, 0],
      },
    },
  ],
}))

const statusOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: {
    bottom: 4,
    icon: 'circle',
    textStyle: { color: '#667085', fontSize: 11 },
  },
  color: ['#d97706', '#247a4b', '#c2414b', '#98a2b3', '#0f8b8d', '#667085'],
  series: [
    {
      type: 'pie',
      radius: ['45%', '68%'],
      center: ['50%', '43%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderColor: '#ffffff',
        borderWidth: 3,
      },
      label: { show: false },
      data: stats.value.statusDistribution.map((item) => ({
        name: item.label,
        value: item.value,
      })),
    },
  ],
}))

const trendOption = computed(() => ({
  grid: { left: 38, right: 20, top: 24, bottom: 30 },
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: stats.value.bookingTrend.map((item) => item.label),
    axisLine: { lineStyle: { color: '#e4e8ef' } },
    axisLabel: { color: '#667085' },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#eef1f5' } },
    axisLabel: { color: '#667085' },
  },
  series: [
    {
      type: 'line',
      smooth: true,
      symbolSize: 7,
      data: stats.value.bookingTrend.map((item) => item.value),
      lineStyle: { width: 3, color: '#0f8b8d' },
      itemStyle: { color: '#0f8b8d' },
      areaStyle: { color: 'rgba(15, 139, 141, 0.10)' },
    },
  ],
}))

async function load() {
  loading.value = true
  try {
    stats.value = await adminApi.stats()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="page">
    <header class="page-header">
      <div>
        <h2 class="page-title">数据统计</h2>
        <p class="page-subtitle">从使用率、预约趋势和状态分布观察会议空间运行情况。</p>
      </div>
      <el-button @click="load"><Gauge :size="16" /> 更新数据</el-button>
    </header>

    <section class="metric-grid">
      <article class="metric" style="--metric-color: #2563eb">
        <div class="metric-label">预约总量</div>
        <div class="metric-value">{{ stats.totalBookings }}</div>
        <div class="metric-note"><CalendarDays :size="14" /> 系统累计记录</div>
      </article>
      <article class="metric" style="--metric-color: #d97706">
        <div class="metric-label">待审批</div>
        <div class="metric-value">{{ stats.pendingBookings }}</div>
        <div class="metric-note"><Clock3 :size="14" /> 当前处理队列</div>
      </article>
      <article class="metric" style="--metric-color: #247a4b">
        <div class="metric-label">已通过</div>
        <div class="metric-value">{{ stats.approvedBookings }}</div>
        <div class="metric-note"><CheckCircle2 :size="14" /> 有效预约记录</div>
      </article>
      <article class="metric" style="--metric-color: #0f8b8d">
        <div class="metric-label">审批通过率</div>
        <div class="metric-value">{{ stats.approvalRate }}%</div>
        <div class="metric-note"><BarChart3 :size="14" /> 按已处理预约计算</div>
      </article>
    </section>

    <section class="chart-grid analytics-grid">
      <ChartPanel
        title="会议室使用排行"
        description="已通过预约数量"
        :option="roomUsageOption"
        :height="330"
      />
      <ChartPanel
        title="预约状态分布"
        description="全部预约记录"
        :option="statusOption"
        :height="330"
      />
    </section>

    <ChartPanel
      title="近 7 日预约趋势"
      description="按预约开始日期统计"
      :option="trendOption"
      :height="300"
    />
  </div>
</template>

<style scoped>
.analytics-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 0.85fr);
  gap: 16px;
}

:deep(.metric-note) {
  display: flex;
  align-items: center;
  gap: 5px;
}

@media (max-width: 1050px) {
  .analytics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
