<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Ban,
  CheckCircle2,
  RefreshCw,
  ShieldCheck,
  UserMinus,
  UsersRound,
  Trash2,
} from '@lucide/vue'
import { adminApi } from '../api'

const loading = ref(false)
const submitting = ref(false)
const users = ref([])
const permissionVisible = ref(false)
const selectedUser = ref(null)
const keyword = ref('')

const permissionForm = reactive({
  allowed: true,
  reason: '',
})

const summary = computed(() => ({
  total: users.value.length,
  approvers: users.value.filter((user) => user.roles.includes('APPROVER')).length,
  blocked: users.value.filter((user) => !user.bookingAllowed).length,
}))

const visibleUsers = computed(() => {
  const term = keyword.value.trim().toLowerCase()
  if (!term) return users.value
  return users.value.filter((user) =>
    [user.username, user.displayName, user.email, user.phone]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(term)),
  )
})

function roleLabel(code) {
  if (code === 'ADMIN') return '系统管理员'
  if (code === 'APPROVER') return '审批管理员'
  return '普通用户'
}

function roleTagType(roles) {
  if (roles.includes('ADMIN')) return 'danger'
  if (roles.includes('APPROVER')) return 'warning'
  return 'info'
}

async function load() {
  loading.value = true
  try {
    users.value = await adminApi.users()
  } finally {
    loading.value = false
  }
}

function openPermission(user) {
  selectedUser.value = user
  permissionForm.allowed = Boolean(user.bookingAllowed)
  permissionForm.reason = user.bookingBlockReason || ''
  permissionVisible.value = true
}

async function savePermission() {
  if (!selectedUser.value) return
  if (!permissionForm.allowed && !permissionForm.reason.trim()) {
    ElMessage.warning('禁止预约时请填写原因')
    return
  }
  submitting.value = true
  try {
    await adminApi.updateBookingPermission(selectedUser.value.id, {
      allowed: permissionForm.allowed,
      reason: permissionForm.allowed ? null : permissionForm.reason.trim(),
    })
    ElMessage.success(permissionForm.allowed ? '已恢复预约权限' : '已禁止预约')
    permissionVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function revokeApprover(user) {
  try {
    await ElMessageBox.confirm(
      `确认撤销“${user.displayName}”的审批管理员权限吗？账号将保留为普通用户。`,
      '撤销审批权限',
      {
        type: 'warning',
        confirmButtonText: '确认撤销',
        cancelButtonText: '返回',
      },
    )
    await adminApi.revokeApprover(user.id)
    ElMessage.success('审批管理员权限已撤销')
    await load()
  } catch {
    // Cancelled by the administrator.
  }
}

async function grantApprover(user) {
  try {
    await ElMessageBox.confirm(
      `确认授予“${user.displayName}”审批管理员权限吗？`,
      '授予审批权限',
      {
        type: 'info',
        confirmButtonText: '确认授予',
        cancelButtonText: '返回',
      },
    )
    await adminApi.grantApprover(user.id)
    ElMessage.success('审批管理员权限已授予')
    await load()
  } catch {
    // Cancelled by the administrator.
  }
}

async function deleteAccount(user) {
  try {
    await ElMessageBox.confirm(
      `确认删除“${user.displayName}”账号吗？账号将被停用且无法再次登录。`,
      '删除账号',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '返回',
      },
    )
    await adminApi.deleteUser(user.id)
    ElMessage.success('账号已删除并停用')
    await load()
  } catch {
    // Cancelled by the administrator.
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h2 class="page-title">人员与权限</h2>
        <p class="page-subtitle">直接为现有用户授予审批权限，并管理会议室预约权限。</p>
      </div>
      <el-button @click="load"><RefreshCw :size="16" /> 刷新</el-button>
    </header>

    <section class="metric-grid">
      <article class="metric" style="--metric-color: #2563eb">
        <div class="metric-label">账号总数</div>
        <div class="metric-value">{{ summary.total }}</div>
        <div class="metric-note"><UsersRound :size="14" /> 全部系统账号</div>
      </article>
      <article class="metric" style="--metric-color: #d97706">
        <div class="metric-label">审批管理员</div>
        <div class="metric-value">{{ summary.approvers }}</div>
        <div class="metric-note"><ShieldCheck :size="14" /> 可处理预约审批</div>
      </article>
      <article class="metric" style="--metric-color: #c2414b">
        <div class="metric-label">禁止预约账号</div>
        <div class="metric-value">{{ summary.blocked }}</div>
        <div class="metric-note"><Ban :size="14" /> 当前受限用户</div>
      </article>
    </section>

    <section class="surface">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索用户名、姓名或联系方式"
        />
      </div>

      <div v-loading="loading" class="personnel-table">
        <el-table :data="visibleUsers" row-key="id">
          <el-table-column label="用户" min-width="220">
            <template #default="{ row }">
              <div class="user-cell">
                <span>{{ row.displayName.slice(0, 1) }}</span>
                <div>
                  <strong>{{ row.displayName }}</strong>
                  <small>{{ row.username }}</small>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="联系方式" min-width="200">
            <template #default="{ row }">
              <div class="contact-cell">
                <span>{{ row.email || '-' }}</span>
                <small>{{ row.phone || '-' }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="角色" min-width="180">
            <template #default="{ row }">
              <el-tag
                v-for="role in row.roles"
                :key="role"
                :type="roleTagType(row.roles)"
                size="small"
                effect="light"
                class="role-tag"
              >
                {{ roleLabel(role) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="预约权限" width="150">
            <template #default="{ row }">
              <el-tag :type="row.bookingAllowed ? 'success' : 'danger'" size="small">
                {{ row.bookingAllowed ? '允许预约' : '禁止预约' }}
              </el-tag>
              <p v-if="!row.bookingAllowed" class="block-reason">
                {{ row.bookingBlockReason }}
              </p>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="340" fixed="right">
            <template #default="{ row }">
              <template v-if="!row.roles.includes('ADMIN')">
                <el-button text type="primary" @click="openPermission(row)">
                  <component
                    :is="row.bookingAllowed ? Ban : CheckCircle2"
                    :size="15"
                  />
                  {{ row.bookingAllowed ? '禁止预约' : '恢复预约' }}
                </el-button>
                <el-button
                  v-if="row.roles.includes('APPROVER')"
                  text
                  type="danger"
                  @click="revokeApprover(row)"
                >
                  <UserMinus :size="15" /> 撤销审批权限
                </el-button>
                <el-button
                  v-else
                  text
                  type="success"
                  :disabled="row.status !== 'ACTIVE'"
                  @click="grantApprover(row)"
                >
                  <ShieldCheck :size="15" /> 授予审批权限
                </el-button>
                <el-button
                  text
                  type="danger"
                  @click="deleteAccount(row)"
                >
                  <Trash2 :size="15" /> 删除账号
                </el-button>
              </template>
              <span v-else class="muted">系统管理员</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <el-dialog
      v-model="permissionVisible"
      :title="permissionForm.allowed ? '禁止预约权限' : '恢复预约权限'"
      width="470px"
    >
      <div v-if="selectedUser" class="permission-user">
        <strong>{{ selectedUser.displayName }}</strong>
        <span>{{ selectedUser.username }}</span>
      </div>
      <el-form label-position="top">
        <el-form-item label="预约权限">
          <el-radio-group v-model="permissionForm.allowed">
            <el-radio-button :value="true">允许预约</el-radio-button>
            <el-radio-button :value="false">禁止预约</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!permissionForm.allowed" label="禁止原因" required>
          <el-input
            v-model="permissionForm.reason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="例如：多次爽约、违规使用或账号暂停"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="permissionVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="savePermission">
          保存权限
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.personnel-table {
  min-height: 360px;
  overflow-x: auto;
}

.user-cell {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
}

.user-cell > span {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 50%;
  background: var(--blue-soft);
  color: var(--blue);
  font-weight: 750;
}

.user-cell div,
.contact-cell {
  display: grid;
}

.user-cell strong {
  font-size: 13px;
}

.user-cell small,
.contact-cell small {
  margin-top: 4px;
  color: var(--muted);
  font-size: 10px;
}

.contact-cell span {
  font-size: 12px;
}

.role-tag + .role-tag {
  margin-left: 5px;
}

.block-reason {
  margin: 5px 0 0;
  color: var(--red);
  font-size: 10px;
  line-height: 1.4;
}

.permission-user {
  display: grid;
  gap: 5px;
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 7px;
  background: #fafbfc;
}

.permission-user span {
  color: var(--muted);
  font-size: 12px;
}

@media (max-width: 720px) {
  .permission-user {
    padding: 10px;
  }
}
</style>
