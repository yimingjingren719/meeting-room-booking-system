<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  BellRing,
  CalendarClock,
  CalendarPlus,
  CheckCircle2,
  Clock3,
  MapPin,
  UsersRound,
} from '@lucide/vue'
import { bookingApi, notificationApi, roomApi, adminApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { formatDateTimeRange, formatDate } from '../utils/format'
import StatusTag from '../components/StatusTag.vue'
import ChartPanel from '../components/ChartPanel.vue'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(true)
const bookings = ref([])
const notifications = ref([])
const stats = ref(null)
const rooms = ref([])

const data = reactive({
  unreadCount: 0,
})

const upcomingBookings = computed(() =>
  bookings.value
    .filter(
      (item) =>
        ['PENDING', 'APPROVED'].includes(item.status) &&
        new Date(item.endTime) > new Date(),
    )
    .slice(0, 4),
)

const pendingCount = computed(
  () => bookings.value.filter((item) => item.status === 'PENDING').length,
)
const approvedCount = computed(
  () => bookings.value.filter((item) => item.status === 'APPROVED').length,
)

const metrics = computed(() => {
  if (stats.value) {
    return [
      {
        label: '预约总量',
        value: stats.value.totalBookings,
        note: '系统内全部预约',
        color: '#2563eb',
      },
      {
        label: '待审批',
        value: stats.value.pendingBookings,
        note: '需要及时处理',
        color: '#d97706',
      },
      {
        label: '今日预约',
        value: stats.value.todayBookings,
        note: formatDate(new Date()),
        color: '#0f8b8d',
      },
      {
        label: '审批通过率',
        value: `${stats.value.approvalRate}%`,
        note: '按已处理预约计算',
        color: '#247a4b',
      },
    ]
  }
  return [
    {
      label: '即将开始',
      value: upcomingBookings.value.length,
      note: '已通过或待审批',
      color: '#2563eb',
    },
    {
      label: '等待审批',
      value: pendingCount.value,
      note: '可继续跟踪',
      color: '#d97706',
    },
    {
      label: '已通过',
      value: approvedCount.value,
      note: '个人有效预约',
      color: '#247a4b',
    },
    {
      label: '未读通知',
      value: data.unreadCount,
      note: '审批与状态变化',
      color: '#0f8b8d',
    },
  ]
})

const trendOption = computed(() => ({
  grid: { left: 38, right: 20, top: 24, bottom: 30 },
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: stats.value?.bookingTrend?.map((item) => item.label) || [],
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
      data: stats.value?.bookingTrend?.map((item) => item.value) || [],
      lineStyle: { width: 3, color: '#2563eb' },
      itemStyle: { color: '#2563eb' },
      areaStyle: { color: 'rgba(37, 99, 235, 0.10)' },
    },
  ],
}))

async function load() {
  loading.value = true
  try {
    const [bookingData, notificationData, roomData] = await Promise.all([
      bookingApi.mine(),
      notificationApi.list(),
      roomApi.list({ date: formatDate(new Date()) }),
    ])
    bookings.value = bookingData
    notifications.value = notificationData
    data.unreadCount = notificationData.filter((item) => !item.read).length
    rooms.value = roomData.slice(0, 3)
    if (auth.isApprover) {
      stats.value = await adminApi.stats()
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="page dashboard-page">
    <section class="welcome-strip">
      <div>
        <p class="welcome-label">预约工作台</p>
        <h2>{{ auth.user?.displayName }}，今天也请高效使用每一个会议空间。</h2>
        <p>快速查看预约进展、审批状态和可用会议室。</p>
      </div>
      <el-button type="primary" size="large" @click="router.push('/rooms')">
        <CalendarPlus :size="18" /> 发起预约
      </el-button>
    </section>

    <section class="metric-grid">
      <article
        v-for="metric in metrics"
        :key="metric.label"
        class="metric"
        :style="{ '--metric-color': metric.color }"
      >
        <div class="metric-label">{{ metric.label }}</div>
        <div class="metric-value">{{ metric.value }}</div>
        <div class="metric-note">{{ metric.note }}</div>
      </article>
    </section>

    <section class="dashboard-grid">
      <div class="surface upcoming-panel">
        <header class="surface-header">
          <div>
            <h2 class="surface-title">即将开始</h2>
            <p class="panel-caption">接下来需要关注的预约</p>
          </div>
          <el-button text @click="router.push('/bookings')">
            查看全部 <ArrowRight :size="15" />
          </el-button>
        </header>

        <div v-if="upcomingBookings.length" class="booking-list">
          <article
            v-for="booking in upcomingBookings"
            :key="booking.id"
            class="booking-row"
          >
            <div class="booking-date">
              <strong>{{ booking.startTime.slice(8, 10) }}</strong>
              <span>{{ booking.startTime.slice(5, 7) }}月</span>
            </div>
            <div class="booking-main">
              <div class="booking-title-line">
                <strong>{{ booking.subject }}</strong>
                <StatusTag :status="booking.status" :text="booking.statusText" />
              </div>
              <p>
                <MapPin :size="14" />
                {{ booking.roomName }} · {{ booking.location }}
              </p>
              <p>
                <Clock3 :size="14" />
                {{ formatDateTimeRange(booking.startTime, booking.endTime) }}
              </p>
            </div>
            <div class="booking-capacity">
              <UsersRound :size="17" />
              {{ booking.attendeeCount }} 人
            </div>
          </article>
        </div>
        <div v-else class="empty-state">
          <div>
            <CalendarClock :size="34" />
            <p>当前没有即将开始的预约</p>
          </div>
        </div>
      </div>

      <div class="surface notification-panel">
        <header class="surface-header">
          <div>
            <h2 class="surface-title">最近通知</h2>
            <p class="panel-caption">审批结果和状态变化</p>
          </div>
          <el-button text @click="router.push('/notifications')">全部</el-button>
        </header>
        <div v-if="notifications.length" class="notice-list">
          <article
            v-for="notice in notifications.slice(0, 5)"
            :key="notice.id"
            class="notice-row"
          >
            <span class="notice-icon" :class="{ unread: !notice.read }">
              <BellRing :size="16" />
            </span>
            <div>
              <strong>{{ notice.title }}</strong>
              <p>{{ notice.content }}</p>
            </div>
          </article>
        </div>
        <div v-else class="empty-state">
          <div>
            <CheckCircle2 :size="32" />
            <p>暂无通知</p>
          </div>
        </div>
      </div>
    </section>

    <section v-if="stats" class="chart-grid">
      <ChartPanel
        title="近 7 日预约趋势"
        description="按预约开始日期统计"
        :option="trendOption"
        :height="280"
      />
      <div class="surface quick-rooms">
        <header class="surface-header">
          <div>
            <h2 class="surface-title">今日可预约会议室</h2>
            <p class="panel-caption">按容量和当前状态推荐</p>
          </div>
          <el-button text @click="router.push('/rooms')">查询更多</el-button>
        </header>
        <div class="quick-room-list">
          <article v-for="room in rooms" :key="room.id" class="quick-room-row">
            <img :src="room.imageUrl" :alt="room.name" />
            <div>
              <strong>{{ room.name }}</strong>
              <p>{{ room.buildingName }} · {{ room.capacity }} 人</p>
            </div>
            <StatusTag :status="room.status" />
          </article>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.welcome-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 24px 26px;
  border: 1px solid #dbe5f5;
  border-left: 4px solid var(--blue);
  border-radius: 8px;
  background: #ffffff;
}

.welcome-label {
  margin: 0 0 7px;
  color: var(--blue);
  font-size: 12px;
  font-weight: 760;
}

.welcome-strip h2 {
  margin: 0;
  font-size: 21px;
  font-weight: 740;
}

.welcome-strip p:last-child {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.dashboard-grid,
.chart-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 0.8fr);
  gap: 16px;
}

.chart-grid {
  grid-template-columns: minmax(0, 1.25fr) minmax(340px, 0.75fr);
}

.panel-caption {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 11px;
}

.booking-list,
.notice-list,
.quick-room-list {
  display: grid;
}

.booking-row {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) auto;
  align-items: center;
  gap: 15px;
  padding: 17px 18px;
  border-bottom: 1px solid var(--line);
}

.booking-row:last-child {
  border-bottom: 0;
}

.booking-date {
  display: grid;
  width: 50px;
  height: 53px;
  place-items: center;
  align-content: center;
  border-radius: 7px;
  background: var(--blue-soft);
  color: var(--blue);
}

.booking-date strong {
  font-size: 19px;
  line-height: 1;
}

.booking-date span {
  margin-top: 4px;
  font-size: 10px;
}

.booking-main {
  min-width: 0;
}

.booking-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.booking-title-line strong {
  overflow: hidden;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.booking-main p {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 7px 0 0;
  color: var(--muted);
  font-size: 12px;
}

.booking-capacity {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #475467;
  font-size: 12px;
}

.notice-row {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  padding: 15px 17px;
  border-bottom: 1px solid var(--line);
}

.notice-row:last-child {
  border-bottom: 0;
}

.notice-icon {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 50%;
  background: #f2f4f7;
  color: #667085;
}

.notice-icon.unread {
  background: var(--blue-soft);
  color: var(--blue);
}

.notice-row strong {
  font-size: 13px;
}

.notice-row p {
  display: -webkit-box;
  margin: 5px 0 0;
  overflow: hidden;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.quick-room-list {
  padding: 6px 16px 12px;
}

.quick-room-row {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) auto;
  align-items: center;
  gap: 11px;
  padding: 12px 0;
  border-bottom: 1px solid var(--line);
}

.quick-room-row:last-child {
  border-bottom: 0;
}

.quick-room-row img {
  width: 54px;
  height: 46px;
  border-radius: 6px;
  object-fit: cover;
}

.quick-room-row strong {
  font-size: 13px;
}

.quick-room-row p {
  margin: 5px 0 0;
  color: var(--muted);
  font-size: 11px;
}

@media (max-width: 1100px) {
  .dashboard-grid,
  .chart-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .welcome-strip {
    align-items: flex-start;
    flex-direction: column;
    padding: 19px;
  }

  .welcome-strip h2 {
    font-size: 17px;
  }

  .booking-row {
    grid-template-columns: 46px minmax(0, 1fr);
  }

  .booking-capacity {
    display: none;
  }
}
</style>
