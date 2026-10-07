<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  CalendarRange,
  CircleX,
  Clock3,
  MapPin,
  Plus,
  UsersRound,
} from '@lucide/vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { bookingApi } from '../api'
import { formatDateTimeRange } from '../utils/format'
import StatusTag from '../components/StatusTag.vue'

const router = useRouter()
const loading = ref(false)
const bookings = ref([])
const activeStatus = ref('')
const cancelVisible = ref(false)
const selectedBooking = ref(null)
const cancelForm = reactive({ reason: '' })

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待审批', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已过期', value: 'EXPIRED' },
  { label: '已取消', value: 'CANCELLED' },
]

const visibleBookings = computed(() => bookings.value)

async function load() {
  loading.value = true
  try {
    bookings.value = await bookingApi.mine({
      status: activeStatus.value || undefined,
    })
  } finally {
    loading.value = false
  }
}

function canCancel(booking) {
  return (
    ['PENDING', 'APPROVED'].includes(booking.status) &&
    new Date(booking.startTime) > new Date()
  )
}

function openCancel(booking) {
  selectedBooking.value = booking
  cancelForm.reason = ''
  cancelVisible.value = true
}

async function confirmCancel() {
  if (!selectedBooking.value) return
  try {
    await ElMessageBox.confirm(
      `确认取消“${selectedBooking.value.subject}”的预约吗？`,
      '取消预约',
      {
        confirmButtonText: '确认取消',
        cancelButtonText: '暂不取消',
        type: 'warning',
      },
    )
    await bookingApi.cancel(selectedBooking.value.id, {
      reason: cancelForm.reason || '用户主动取消',
    })
    ElMessage.success('预约已取消')
    cancelVisible.value = false
    await load()
  } catch {
    // The user cancelled the confirmation dialog.
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h2 class="page-title">我的预约</h2>
        <p class="page-subtitle">跟踪申请进度、审批意见和会议时间。</p>
      </div>
      <el-button type="primary" @click="router.push('/rooms')">
        <Plus :size="16" /> 新建预约
      </el-button>
    </header>

    <section class="surface">
      <div class="booking-filter">
        <el-segmented v-model="activeStatus" :options="statusOptions" @change="load" />
      </div>

      <div v-loading="loading" class="booking-table-wrap">
        <el-table
          v-if="visibleBookings.length"
          :data="visibleBookings"
          style="width: 100%"
          row-key="id"
        >
          <el-table-column label="会议信息" min-width="260">
            <template #default="{ row }">
              <div class="booking-subject">
                <strong>{{ row.subject }}</strong>
                <span>{{ row.bookingNo }}</span>
                <small v-if="row.purpose">{{ row.purpose }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="会议室" min-width="190">
            <template #default="{ row }">
              <div class="booking-room-cell">
                <strong>{{ row.roomName }}</strong>
                <span><MapPin :size="13" /> {{ row.location }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="预约时间" min-width="220">
            <template #default="{ row }">
              <div class="time-cell">
                <Clock3 :size="15" />
                <span>{{ formatDateTimeRange(row.startTime, row.endTime) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="参会人数" width="100" align="center">
            <template #default="{ row }">
              <span class="capacity-cell"><UsersRound :size="14" /> {{ row.attendeeCount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <StatusTag :status="row.status" :text="row.statusText" />
            </template>
          </el-table-column>
          <el-table-column label="审批信息" min-width="170">
            <template #default="{ row }">
              <div v-if="row.approvalComment" class="approval-note">
                <span>{{ row.approverName || '审批管理员' }}</span>
                <small>{{ row.approvalComment }}</small>
              </div>
              <span v-else class="muted">等待处理</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="105" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="canCancel(row)"
                text
                type="danger"
                @click="openCancel(row)"
              >
                取消预约
              </el-button>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
        </el-table>

        <div v-else-if="!loading" class="empty-state">
          <div>
            <CalendarRange :size="38" />
            <p>当前筛选条件下没有预约记录</p>
            <el-button type="primary" plain @click="router.push('/rooms')">
              去查询会议室
            </el-button>
          </div>
        </div>
      </div>
    </section>

    <el-dialog v-model="cancelVisible" title="取消预约" width="440px">
      <div v-if="selectedBooking" class="cancel-booking-info">
        <strong>{{ selectedBooking.subject }}</strong>
        <span>{{ selectedBooking.roomName }}</span>
        <span>{{ formatDateTimeRange(selectedBooking.startTime, selectedBooking.endTime) }}</span>
      </div>
      <el-form label-position="top">
        <el-form-item label="取消原因">
          <el-input
            v-model="cancelForm.reason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="选填，便于审批管理员了解情况"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelVisible = false">返回</el-button>
        <el-button type="danger" @click="confirmCancel">
          <CircleX :size="16" /> 确认取消
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.booking-filter {
  padding: 15px 18px;
  overflow-x: auto;
  border-bottom: 1px solid var(--line);
}

.booking-table-wrap {
  min-height: 300px;
  overflow-x: auto;
}

.booking-subject,
.booking-room-cell,
.approval-note {
  display: grid;
}

.booking-subject strong {
  font-size: 13px;
}

.booking-subject span {
  margin-top: 4px;
  color: #98a2b3;
  font-size: 10px;
  font-family: ui-monospace, "SFMono-Regular", Consolas, monospace;
}

.booking-subject small {
  display: -webkit-box;
  margin-top: 7px;
  overflow: hidden;
  color: var(--muted);
  font-size: 11px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.booking-room-cell strong {
  font-size: 12px;
}

.booking-room-cell span,
.time-cell,
.capacity-cell {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--muted);
  font-size: 11px;
}

.booking-room-cell span {
  margin-top: 6px;
}

.capacity-cell {
  justify-content: center;
}

.approval-note span {
  font-size: 11px;
  font-weight: 700;
}

.approval-note small {
  display: -webkit-box;
  margin-top: 4px;
  overflow: hidden;
  color: var(--muted);
  font-size: 10px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.cancel-booking-info {
  display: grid;
  gap: 6px;
  margin-bottom: 17px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 7px;
  background: #fafbfc;
}

.cancel-booking-info strong {
  font-size: 14px;
}

.cancel-booking-info span {
  color: var(--muted);
  font-size: 12px;
}

@media (max-width: 720px) {
  .booking-filter {
    padding: 12px;
  }

  :deep(.el-segmented) {
    min-width: 500px;
  }
}
</style>
