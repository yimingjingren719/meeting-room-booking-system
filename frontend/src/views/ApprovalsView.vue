<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Check,
  ClipboardCheck,
  Clock3,
  MapPin,
  MessageSquareText,
  RotateCw,
  UsersRound,
  X,
} from '@lucide/vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../api'
import { formatDateTimeRange } from '../utils/format'
import StatusTag from '../components/StatusTag.vue'

const loading = ref(false)
const submitting = ref(false)
const bookings = ref([])
const activeStatus = ref('PENDING')
const dialogVisible = ref(false)
const selectedBooking = ref(null)

const form = reactive({
  approved: true,
  comment: '',
})

const statusOptions = [
  { label: '待审批', value: 'PENDING' },
  { label: '全部', value: '' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
]

const pendingCount = computed(
  () => bookings.value.filter((item) => item.status === 'PENDING').length,
)

async function load() {
  loading.value = true
  try {
    bookings.value = await adminApi.bookings({
      status: activeStatus.value || undefined,
    })
  } finally {
    loading.value = false
  }
}

function openApproval(booking, approved) {
  selectedBooking.value = booking
  form.approved = approved
  form.comment = ''
  dialogVisible.value = true
}

async function submitApproval() {
  if (!selectedBooking.value) return
  if (!form.approved && !form.comment.trim()) {
    ElMessage.warning('驳回预约时请填写审批意见')
    return
  }
  submitting.value = true
  try {
    await adminApi.approve(selectedBooking.value.id, {
      approved: form.approved,
      comment: form.comment.trim() || null,
    })
    ElMessage.success(form.approved ? '预约已通过' : '预约已驳回')
    dialogVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h2 class="page-title">审批中心</h2>
        <p class="page-subtitle">
          处理责任范围内的预约申请，系统会在审批前再次验证时间冲突。
        </p>
      </div>
      <el-button @click="load"><RotateCw :size="16" /> 刷新列表</el-button>
    </header>

    <section class="metric-grid approval-metrics">
      <article class="metric" style="--metric-color: #d97706">
        <div class="metric-label">当前列表</div>
        <div class="metric-value">{{ bookings.length }}</div>
        <div class="metric-note">条预约记录</div>
      </article>
      <article class="metric" style="--metric-color: #2563eb">
        <div class="metric-label">待处理</div>
        <div class="metric-value">{{ pendingCount }}</div>
        <div class="metric-note">需及时给出审批结果</div>
      </article>
    </section>

    <section class="surface">
      <div class="approval-toolbar">
        <el-segmented v-model="activeStatus" :options="statusOptions" @change="load" />
      </div>

      <div v-loading="loading" class="approval-list">
        <article v-for="booking in bookings" :key="booking.id" class="approval-row">
          <div class="approval-date">
            <span>{{ booking.startTime.slice(5, 7) }} 月</span>
            <strong>{{ booking.startTime.slice(8, 10) }}</strong>
          </div>
          <div class="approval-content">
            <div class="approval-title">
              <h3>{{ booking.subject }}</h3>
              <StatusTag :status="booking.status" :text="booking.statusText" />
            </div>
            <p class="approval-purpose">{{ booking.purpose || '未填写用途说明' }}</p>
            <div class="approval-meta">
              <span><MapPin :size="14" /> {{ booking.roomName }} · {{ booking.location }}</span>
              <span><Clock3 :size="14" /> {{ formatDateTimeRange(booking.startTime, booking.endTime) }}</span>
              <span><UsersRound :size="14" /> {{ booking.attendeeCount }} 人</span>
            </div>
            <div class="applicant-line">
              <span>申请人：{{ booking.applicantName }}</span>
              <span>提交编号：{{ booking.bookingNo }}</span>
            </div>
          </div>
          <div class="approval-actions">
            <template v-if="booking.status === 'PENDING'">
              <el-button type="success" plain @click="openApproval(booking, true)">
                <Check :size="16" /> 通过
              </el-button>
              <el-button type="danger" plain @click="openApproval(booking, false)">
                <X :size="16" /> 驳回
              </el-button>
            </template>
            <div v-else-if="booking.approvalComment" class="processed-comment">
              <MessageSquareText :size="15" />
              <span>{{ booking.approvalComment }}</span>
            </div>
          </div>
        </article>

        <div v-if="!loading && !bookings.length" class="empty-state">
          <div>
            <ClipboardCheck :size="38" />
            <p>当前没有需要处理的预约</p>
          </div>
        </div>
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="form.approved ? '通过预约申请' : '驳回预约申请'"
      width="500px"
    >
      <div v-if="selectedBooking" class="approval-dialog-summary">
        <strong>{{ selectedBooking.subject }}</strong>
        <span>{{ selectedBooking.applicantName }} · {{ selectedBooking.roomName }}</span>
        <span>{{ formatDateTimeRange(selectedBooking.startTime, selectedBooking.endTime) }}</span>
      </div>

      <el-form label-position="top">
        <el-form-item label="审批意见" :required="!form.approved">
          <el-input
            v-model="form.comment"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            :placeholder="form.approved ? '选填，可补充使用要求' : '请说明驳回原因'"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          :type="form.approved ? 'success' : 'danger'"
          :loading="submitting"
          @click="submitApproval"
        >
          {{ form.approved ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.approval-metrics {
  grid-template-columns: repeat(2, minmax(0, 220px));
}

.approval-toolbar {
  padding: 15px 18px;
  overflow-x: auto;
  border-bottom: 1px solid var(--line);
}

.approval-list {
  min-height: 300px;
}

.approval-row {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
  padding: 20px;
  border-bottom: 1px solid var(--line);
}

.approval-row:last-child {
  border-bottom: 0;
}

.approval-date {
  display: grid;
  width: 60px;
  height: 67px;
  place-items: center;
  align-content: center;
  border-radius: 7px;
  background: var(--blue-soft);
  color: var(--blue);
}

.approval-date span {
  font-size: 10px;
}

.approval-date strong {
  margin-top: 2px;
  font-size: 23px;
  line-height: 1;
}

.approval-content {
  min-width: 0;
}

.approval-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.approval-title h3 {
  margin: 0;
  font-size: 15px;
}

.approval-purpose {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.55;
}

.approval-meta,
.applicant-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 17px;
  margin-top: 11px;
}

.approval-meta span {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #475467;
  font-size: 11px;
}

.applicant-line {
  margin-top: 10px;
  color: #98a2b3;
  font-size: 10px;
}

.approval-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.processed-comment {
  display: flex;
  max-width: 220px;
  align-items: flex-start;
  gap: 6px;
  color: var(--muted);
  font-size: 11px;
  line-height: 1.5;
}

.approval-dialog-summary {
  display: grid;
  gap: 6px;
  margin-bottom: 17px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 7px;
  background: #fafbfc;
}

.approval-dialog-summary strong {
  font-size: 14px;
}

.approval-dialog-summary span {
  color: var(--muted);
  font-size: 12px;
}

@media (max-width: 800px) {
  .approval-row {
    grid-template-columns: 54px minmax(0, 1fr);
    gap: 12px;
    padding: 16px;
  }

  .approval-date {
    width: 52px;
    height: 60px;
  }

  .approval-actions {
    grid-column: 2;
    justify-content: flex-start;
  }

  .processed-comment {
    max-width: none;
  }
}
</style>
