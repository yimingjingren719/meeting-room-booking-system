<script setup>
import { computed, onMounted, ref } from 'vue'
import { FileClock, RefreshCw, Search, ShieldCheck } from '@lucide/vue'
import { adminApi } from '../api'
import { formatDateTime } from '../utils/format'

const loading = ref(false)
const logs = ref([])
const keyword = ref('')

const actionLabels = {
  LOGIN: '用户登录',
  CREATE_BOOKING: '提交预约',
  APPROVE_BOOKING: '通过预约',
  REJECT_BOOKING: '驳回预约',
  CANCEL_BOOKING: '取消预约',
  UPDATE_ROOM_STATUS: '修改会议室状态',
  CREATE_UNAVAILABLE: '新增维护时段',
  GRANT_APPROVER: '恢复审批权限',
  REVOKE_APPROVER: '撤销审批权限',
  DELETE_USER: '删除并停用账号',
  UPDATE_BOOKING_PERMISSION: '修改预约权限',
}

const filteredLogs = computed(() => {
  const term = keyword.value.trim().toLowerCase()
  if (!term) return logs.value
  return logs.value.filter((log) =>
    [
      log.operatorName,
      actionLabels[log.action] || log.action,
      log.targetType,
      log.targetId,
      log.detail,
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(term)),
  )
})

async function load() {
  loading.value = true
  try {
    logs.value = await adminApi.auditLogs()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h2 class="page-title">审计日志</h2>
        <p class="page-subtitle">追踪预约、审批和取消等关键操作，保留最近 100 条记录。</p>
      </div>
      <el-button @click="load"><RefreshCw :size="16" /> 刷新</el-button>
    </header>

    <section class="surface audit-summary">
      <div class="audit-summary-icon"><ShieldCheck :size="24" /></div>
      <div>
        <strong>关键操作可追溯</strong>
        <p>日志记录操作人、对象、结果和时间，便于定位权限或流程问题。</p>
      </div>
      <span>{{ logs.length }} 条记录</span>
    </section>

    <section class="surface">
      <div class="toolbar">
        <el-input v-model="keyword" clearable placeholder="搜索操作人、对象或详情">
          <template #prefix><Search :size="16" /></template>
        </el-input>
      </div>

      <div v-loading="loading" class="audit-table-wrap">
        <el-table v-if="filteredLogs.length" :data="filteredLogs" row-key="id">
          <el-table-column label="时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="operatorName" label="操作人" width="120" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">
                {{ actionLabels[row.action] || row.action }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="对象" min-width="170">
            <template #default="{ row }">
              {{ row.targetType }} #{{ row.targetId || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="结果" width="100">
            <template #default="{ row }">
              <el-tag
                size="small"
                :type="row.result === 'SUCCESS' ? 'success' : 'danger'"
              >
                {{ row.result === 'SUCCESS' ? '成功' : row.result }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="detail" label="详情" min-width="260">
            <template #default="{ row }">
              <span class="log-detail">{{ row.detail || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>
        <div v-else-if="!loading" class="empty-state">
          <div>
            <FileClock :size="38" />
            <p>没有匹配的审计记录</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.audit-summary {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  border-left: 4px solid var(--teal);
}

.audit-summary-icon {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 8px;
  background: var(--teal-soft);
  color: var(--teal);
}

.audit-summary strong {
  font-size: 14px;
}

.audit-summary p {
  margin: 5px 0 0;
  color: var(--muted);
  font-size: 12px;
}

.audit-summary > span {
  color: var(--muted);
  font-size: 12px;
}

.audit-table-wrap {
  min-height: 300px;
  overflow-x: auto;
}

.log-detail {
  display: -webkit-box;
  overflow: hidden;
  color: var(--muted);
  font-size: 11px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

@media (max-width: 680px) {
  .audit-summary {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .audit-summary > span {
    grid-column: 2;
  }
}
</style>

