import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AppLayout from '../layouts/AppLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: AppLayout,
    children: [
      {
        path: '',
        name: 'dashboard',
        component: () => import('../views/DashboardView.vue'),
        meta: { title: '工作台' },
      },
      {
        path: 'rooms',
        name: 'rooms',
        component: () => import('../views/RoomsView.vue'),
        meta: { title: '会议室查询' },
      },
      {
        path: 'bookings',
        name: 'bookings',
        component: () => import('../views/BookingsView.vue'),
        meta: { title: '我的预约' },
      },
      {
        path: 'approvals',
        name: 'approvals',
        component: () => import('../views/ApprovalsView.vue'),
        meta: { title: '审批中心', roles: ['APPROVER', 'ADMIN'] },
      },
      {
        path: 'analytics',
        name: 'analytics',
        component: () => import('../views/AnalyticsView.vue'),
        meta: { title: '数据统计', roles: ['APPROVER', 'ADMIN'] },
      },
      {
        path: 'resources',
        name: 'resources',
        component: () => import('../views/ResourcesView.vue'),
        meta: { title: '资源维护', roles: ['ADMIN'] },
      },
      {
        path: 'audit',
        name: 'audit',
        component: () => import('../views/AuditView.vue'),
        meta: { title: '审计日志', roles: ['ADMIN'] },
      },
      {
        path: 'notifications',
        name: 'notifications',
        component: () => import('../views/NotificationsView.vue'),
        meta: { title: '消息通知' },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('../views/ProfileView.vue'),
        meta: { title: '个人资料' },
      },
      {
        path: 'personnel',
        name: 'personnel',
        component: () => import('../views/PersonnelView.vue'),
        meta: { title: '人员与权限', roles: ['ADMIN'] },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    return auth.isAuthenticated ? '/' : true
  }
  if (!auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (!auth.user || auth.user.bookingAllowed === undefined) {
    try {
      await auth.fetchMe()
    } catch {
      auth.logout()
      return { name: 'login' }
    }
  }
  const allowedRoles = to.meta.roles
  if (allowedRoles && !allowedRoles.some((role) => auth.roles.includes(role))) {
    return { name: 'dashboard' }
  }
  return true
})

export default router
