<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  Building2,
  CalendarCheck2,
  CalendarRange,
  ChartNoAxesCombined,
  ChevronDown,
  ClipboardCheck,
  LayoutDashboard,
  LogOut,
  ScrollText,
  ShieldCheck,
  UserRound,
  Wrench,
} from '@lucide/vue'
import { ElMessage } from 'element-plus'
import { notificationApi } from '../api'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const unreadCount = ref(0)
let refreshTimer

const roleItems = computed(() => [
  {
    label: '工作台',
    route: 'dashboard',
    icon: LayoutDashboard,
  },
  {
    label: '会议室查询',
    route: 'rooms',
    icon: Building2,
  },
  {
    label: '我的预约',
    route: 'bookings',
    icon: CalendarRange,
  },
  {
    label: '审批中心',
    route: 'approvals',
    icon: ClipboardCheck,
    visible: auth.isApprover,
  },
  {
    label: '数据统计',
    route: 'analytics',
    icon: ChartNoAxesCombined,
    visible: auth.isApprover,
  },
  {
    label: '资源维护',
    route: 'resources',
    icon: Wrench,
    visible: auth.isAdmin,
  },
  {
    label: '人员与权限',
    route: 'personnel',
    icon: ShieldCheck,
    visible: auth.isAdmin,
  },
  {
    label: '审计日志',
    route: 'audit',
    icon: ScrollText,
    visible: auth.isAdmin,
  },
].filter((item) => item.visible !== false))

const roleLabel = computed(() => {
  if (auth.isAdmin) return '系统管理员'
  if (auth.roles.includes('APPROVER')) return '审批管理员'
  return '普通用户'
})

const displayDate = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long',
}).format(new Date())

async function loadUnread() {
  if (!auth.isAuthenticated) return
  try {
    unreadCount.value = await notificationApi.unreadCount()
  } catch {
    unreadCount.value = 0
  }
}

function handleUserCommand(command) {
  if (command === 'logout') {
    auth.logout()
    ElMessage.success('已退出登录')
    router.replace('/login')
    return
  }
  if (command === 'notifications') {
    router.push('/notifications')
    return
  }
  if (command === 'profile') {
    router.push('/profile')
  }
}

onMounted(() => {
  loadUnread()
  refreshTimer = window.setInterval(loadUnread, 60_000)
})

onBeforeUnmount(() => {
  window.clearInterval(refreshTimer)
})
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark"><CalendarCheck2 :size="21" /></span>
        <span class="brand-copy">
          <strong>会议室预约</strong>
          <small>USST Meeting Hub</small>
        </span>
      </div>

      <nav class="nav-list" aria-label="主导航">
        <router-link
          v-for="item in roleItems"
          :key="item.route"
          class="nav-item"
          :to="{ name: item.route }"
        >
          <component :is="item.icon" :size="18" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <span class="account-avatar">{{ auth.user?.displayName?.slice(0, 1) }}</span>
        <span class="account-copy">
          <strong>{{ auth.user?.displayName }}</strong>
          <small>{{ roleLabel }}</small>
        </span>
      </div>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <div>
          <p class="topbar-date">{{ displayDate }}</p>
          <h1 class="topbar-title">{{ route.meta.title || '会议室预约' }}</h1>
        </div>

        <div class="topbar-actions">
          <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99">
            <button
              class="icon-action"
              type="button"
              aria-label="查看通知"
              @click="router.push('/notifications')"
            >
              <Bell :size="19" />
            </button>
          </el-badge>

          <el-dropdown trigger="click" @command="handleUserCommand">
            <button class="user-action" type="button">
              <span class="user-avatar">{{ auth.user?.displayName?.slice(0, 1) }}</span>
              <span class="user-meta">
                <strong>{{ auth.user?.displayName }}</strong>
                <small>{{ roleLabel }}</small>
              </span>
              <ChevronDown :size="15" />
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <UserRound :size="15" /> 个人资料
                </el-dropdown-item>
                <el-dropdown-item command="notifications">
                  <Bell :size="15" /> 消息通知
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <LogOut :size="15" /> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="main-content">
        <router-view />
      </main>
    </section>
  </div>
</template>

<style scoped>
.app-shell {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  min-height: 100vh;
}

.sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 20;
  display: flex;
  width: 248px;
  flex-direction: column;
  padding: 18px 14px;
  background: var(--navy);
  color: #d9e1f0;
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 4px 7px 20px;
}

.brand-mark {
  display: grid;
  width: 37px;
  height: 37px;
  place-items: center;
  flex: 0 0 auto;
  border-radius: 8px;
  background: #2f6fed;
  color: white;
}

.brand-copy,
.account-copy,
.user-meta {
  display: grid;
  min-width: 0;
}

.brand-copy strong {
  color: #ffffff;
  font-size: 15px;
}

.brand-copy small {
  margin-top: 2px;
  color: #8fa0bd;
  font-size: 10px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.nav-list {
  display: grid;
  gap: 5px;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  min-height: 43px;
  padding: 0 12px;
  border-radius: 7px;
  color: #aebbd0;
  font-size: 14px;
  font-weight: 620;
  transition:
    background 0.16s ease,
    color 0.16s ease;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #ffffff;
}

.nav-item.router-link-exact-active {
  background: var(--navy-soft);
  color: #ffffff;
}

.nav-item.router-link-exact-active::before {
  position: absolute;
  left: 0;
  width: 3px;
  height: 22px;
  border-radius: 0 3px 3px 0;
  background: #5a8cff;
  content: "";
}

.sidebar-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: auto;
  padding: 14px 8px 4px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.account-avatar,
.user-avatar {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border-radius: 50%;
  background: var(--blue-soft);
  color: var(--blue);
  font-weight: 760;
}

.account-avatar {
  width: 34px;
  height: 34px;
}

.account-copy strong {
  overflow: hidden;
  color: white;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-copy small {
  color: #8fa0bd;
  font-size: 11px;
}

.workspace {
  grid-column: 2;
  min-width: 0;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 15;
  display: flex;
  min-height: 78px;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 12px 26px;
  border-bottom: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(12px);
}

.topbar-date {
  margin: 0 0 3px;
  color: var(--muted);
  font-size: 12px;
}

.topbar-title {
  margin: 0;
  font-size: 18px;
  font-weight: 730;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-action,
.user-action {
  border: 1px solid var(--line);
  background: white;
  color: var(--ink);
  cursor: pointer;
}

.icon-action {
  display: grid;
  width: 39px;
  height: 39px;
  place-items: center;
  border-radius: 8px;
}

.icon-action:hover,
.user-action:hover {
  border-color: #c7d2e4;
  background: #f9fafb;
}

.user-action {
  display: flex;
  align-items: center;
  gap: 9px;
  min-height: 43px;
  padding: 4px 9px 4px 5px;
  border-radius: 8px;
}

.user-avatar {
  width: 33px;
  height: 33px;
}

.user-meta {
  min-width: 88px;
  text-align: left;
}

.user-meta strong {
  font-size: 12px;
}

.user-meta small {
  color: var(--muted);
  font-size: 10px;
}

.main-content {
  width: min(1480px, 100%);
  margin: 0 auto;
  padding: 24px 26px 40px;
}

@media (max-width: 900px) {
  .app-shell {
    display: block;
  }

  .sidebar {
    position: sticky;
    top: 0;
    width: 100%;
    height: auto;
    padding: 10px 12px 8px;
  }

  .brand {
    padding: 0 4px 9px;
  }

  .brand-mark {
    width: 32px;
    height: 32px;
  }

  .brand-copy small,
  .sidebar-footer,
  .nav-item.router-link-exact-active::before {
    display: none;
  }

  .nav-list {
    display: flex;
    gap: 5px;
    overflow-x: auto;
    padding-bottom: 2px;
    scrollbar-width: none;
  }

  .nav-list::-webkit-scrollbar {
    display: none;
  }

  .nav-item {
    min-width: max-content;
    min-height: 38px;
    padding: 0 10px;
    font-size: 12px;
  }

  .workspace {
    grid-column: auto;
  }

  .topbar {
    top: 102px;
    min-height: 66px;
    padding: 10px 14px;
  }

  .topbar-date,
  .user-meta {
    display: none;
  }

  .user-action {
    min-height: 39px;
    padding: 3px;
  }

  .user-action > svg {
    display: none;
  }

  .main-content {
    padding: 16px 14px 30px;
  }
}
</style>

