<script setup>
import { computed, onMounted, ref } from 'vue'
import { Bell, BellRing, CheckCheck, ExternalLink } from '@lucide/vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { notificationApi } from '../api'
import { formatDateTime } from '../utils/format'

const router = useRouter()
const loading = ref(false)
const notifications = ref([])
const activeFilter = ref('all')

const filterOptions = [
  { label: '全部消息', value: 'all' },
  { label: '未读消息', value: 'unread' },
  { label: '已读消息', value: 'read' },
]

const visibleNotifications = computed(() => {
  if (activeFilter.value === 'unread') {
    return notifications.value.filter((item) => !item.read)
  }
  if (activeFilter.value === 'read') {
    return notifications.value.filter((item) => item.read)
  }
  return notifications.value
})

const unreadCount = computed(
  () => notifications.value.filter((item) => !item.read).length,
)

async function load() {
  loading.value = true
  try {
    notifications.value = await notificationApi.list()
  } finally {
    loading.value = false
  }
}

async function markRead(notification) {
  if (notification.read) return
  await notificationApi.markRead(notification.id)
  notification.read = true
}

async function markAllRead() {
  await notificationApi.markAllRead()
  notifications.value.forEach((item) => {
    item.read = true
  })
  ElMessage.success('全部消息已标记为已读')
}

async function openBooking(notification) {
  await markRead(notification)
  router.push('/bookings')
}

onMounted(load)
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h2 class="page-title">消息通知</h2>
        <p class="page-subtitle">
          共 {{ notifications.length }} 条消息，其中 {{ unreadCount }} 条未读。
        </p>
      </div>
      <el-button :disabled="!unreadCount" @click="markAllRead">
        <CheckCheck :size="16" /> 全部已读
      </el-button>
    </header>

    <section class="surface">
      <div class="notification-toolbar">
        <el-segmented v-model="activeFilter" :options="filterOptions" />
      </div>

      <div v-loading="loading" class="notification-list">
        <article
          v-for="notice in visibleNotifications"
          :key="notice.id"
          class="notification-item"
          :class="{ unread: !notice.read }"
          @click="markRead(notice)"
        >
          <span class="notification-icon">
            <BellRing v-if="!notice.read" :size="18" />
            <Bell v-else :size="18" />
          </span>
          <div class="notification-body">
            <div class="notification-title">
              <strong>{{ notice.title }}</strong>
              <span>{{ formatDateTime(notice.createdAt) }}</span>
            </div>
            <p>{{ notice.content }}</p>
            <button
              v-if="notice.bookingId"
              type="button"
              class="notification-link"
              @click.stop="openBooking(notice)"
            >
              查看预约 <ExternalLink :size="13" />
            </button>
          </div>
          <span v-if="!notice.read" class="unread-mark" />
        </article>

        <div v-if="!loading && !visibleNotifications.length" class="empty-state">
          <div>
            <Bell :size="38" />
            <p>当前没有符合条件的消息</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.notification-toolbar {
  padding: 15px 18px;
  overflow-x: auto;
  border-bottom: 1px solid var(--line);
}

.notification-list {
  min-height: 300px;
}

.notification-item {
  position: relative;
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 13px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--line);
  cursor: pointer;
}

.notification-item:last-child {
  border-bottom: 0;
}

.notification-item.unread {
  background: #f8fbff;
}

.notification-icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 50%;
  background: #f2f4f7;
  color: #667085;
}

.notification-item.unread .notification-icon {
  background: var(--blue-soft);
  color: var(--blue);
}

.notification-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 15px;
}

.notification-title strong {
  font-size: 14px;
}

.notification-title span {
  flex: 0 0 auto;
  color: #98a2b3;
  font-size: 11px;
}

.notification-body p {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.65;
}

.notification-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 10px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--blue);
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}

.unread-mark {
  position: absolute;
  top: 22px;
  right: 12px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--blue);
}

@media (max-width: 620px) {
  .notification-title {
    align-items: flex-start;
    flex-direction: column;
    gap: 5px;
  }
}
</style>
