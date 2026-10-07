<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  BadgeAlert,
  Building2,
  CalendarDays,
  Clock3,
  MapPin,
  MonitorSmartphone,
  Search,
  Sparkles,
  Star,
  UsersRound,
} from '@lucide/vue'
import { ElMessage } from 'element-plus'
import { bookingApi, roomApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { dateTimeInput, formatDate } from '../utils/format'
import StatusTag from '../components/StatusTag.vue'

const loading = ref(false)
const submitting = ref(false)
const recommendLoading = ref(false)
const rooms = ref([])
const recommendations = ref([])
const buildings = ref([])
const equipmentOptions = ref([])
const dialogVisible = ref(false)
const recommendVisible = ref(false)
const selectedRoom = ref(null)
const formRef = ref()
const auth = useAuthStore()

const filters = reactive({
  buildingId: null,
  capacity: null,
  equipment: '',
  date: formatDate(new Date()),
})

const bookingForm = reactive({
  subject: '',
  purpose: '',
  attendeeCount: 1,
  timeRange: [],
})

const recommendForm = reactive({
  attendeeCount: 5,
  equipment: '',
  timeRange: [],
})

const bookingRules = {
  subject: [{ required: true, message: '请输入会议主题', trigger: 'blur' }],
  attendeeCount: [{ required: true, message: '请输入参会人数', trigger: 'change' }],
  timeRange: [
    {
      type: 'array',
      required: true,
      min: 2,
      message: '请选择预约时间',
      trigger: 'change',
    },
  ],
}

const availableCount = computed(
  () =>
    rooms.value.filter((room) =>
      room.availability?.some((slot) => slot.status === 'AVAILABLE'),
    ).length,
)

const disabledDate = (date) => date.getTime() < Date.now() - 24 * 60 * 60 * 1000

async function loadOptions() {
  const [buildingData, equipmentData] = await Promise.all([
    roomApi.buildings(),
    roomApi.equipment(),
  ])
  buildings.value = buildingData
  equipmentOptions.value = equipmentData
}

async function loadRooms() {
  loading.value = true
  try {
    rooms.value = await roomApi.list({
      buildingId: filters.buildingId || undefined,
      capacity: filters.capacity || undefined,
      equipment: filters.equipment || undefined,
      date: formatDate(filters.date),
    })
  } finally {
    loading.value = false
  }
}

function firstAvailableSlot(room) {
  return room.availability?.find((slot) => slot.status === 'AVAILABLE')
}

function openBooking(room, slot) {
  if (!auth.user?.bookingAllowed) {
    ElMessage.warning(auth.user?.bookingBlockReason || '当前账号已被禁止预约')
    return
  }
  selectedRoom.value = room
  const targetSlot = slot || firstAvailableSlot(room)
  bookingForm.subject = ''
  bookingForm.purpose = ''
  bookingForm.attendeeCount = Math.min(2, room.capacity)
  bookingForm.timeRange = targetSlot
    ? [
        dateTimeInput(filters.date, targetSlot.startTime.slice(0, 5)),
        dateTimeInput(filters.date, targetSlot.endTime.slice(0, 5)),
      ]
    : []
  dialogVisible.value = true
}

async function toggleFavorite(room) {
  if (room.favorite) {
    await roomApi.unfavorite(room.id)
    room.favorite = false
    ElMessage.success('已取消收藏')
  } else {
    await roomApi.favorite(room.id)
    room.favorite = true
    ElMessage.success('已加入常用会议室')
  }
}

function openRecommendation() {
  recommendForm.attendeeCount = filters.capacity || 5
  recommendForm.equipment = filters.equipment
  recommendForm.timeRange = []
  recommendations.value = []
  recommendVisible.value = true
}

async function loadRecommendations() {
  if (recommendForm.timeRange.length !== 2) {
    ElMessage.warning('请选择需要使用的起止时间')
    return
  }
  recommendLoading.value = true
  try {
    const [start, end] = recommendForm.timeRange
    recommendations.value = await roomApi.recommendations({
      attendeeCount: recommendForm.attendeeCount,
      equipment: recommendForm.equipment || undefined,
      date: start.slice(0, 10),
      startTime: start.slice(11, 16),
      endTime: end.slice(11, 16),
    })
  } finally {
    recommendLoading.value = false
  }
}

function openRecommendedBooking(room) {
  if (!auth.user?.bookingAllowed) {
    ElMessage.warning(auth.user?.bookingBlockReason || '当前账号已被禁止预约')
    return
  }
  selectedRoom.value = room
  bookingForm.subject = ''
  bookingForm.purpose = ''
  bookingForm.attendeeCount = recommendForm.attendeeCount
  bookingForm.timeRange = [...recommendForm.timeRange]
  recommendVisible.value = false
  dialogVisible.value = true
}

async function submitBooking() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (!selectedRoom.value) return
  submitting.value = true
  try {
    await bookingApi.create({
      roomId: selectedRoom.value.id,
      subject: bookingForm.subject,
      purpose: bookingForm.purpose,
      attendeeCount: bookingForm.attendeeCount,
      startTime: bookingForm.timeRange[0],
      endTime: bookingForm.timeRange[1],
    })
    ElMessage.success('预约申请已提交，等待审批')
    dialogVisible.value = false
    await loadRooms()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadOptions(), loadRooms()])
})
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h2 class="page-title">会议室查询</h2>
        <p class="page-subtitle">
          {{ formatDate(filters.date) }} 共 {{ rooms.length }} 间会议室，
          {{ availableCount }} 间仍有空闲时段
        </p>
      </div>
      <div class="header-actions">
        <el-button @click="loadRooms">
          <Search :size="16" /> 刷新结果
        </el-button>
        <el-button type="primary" @click="openRecommendation">
          <Sparkles :size="16" /> 智能推荐
        </el-button>
      </div>
    </header>

    <el-alert
      v-if="auth.user && !auth.user.bookingAllowed"
      type="warning"
      :closable="false"
      show-icon
      :title="`当前账号已被禁止预约：${auth.user.bookingBlockReason || '请联系系统管理员'}`"
    />

    <section class="surface">
      <div class="toolbar">
        <el-date-picker
          v-model="filters.date"
          type="date"
          value-format="YYYY-MM-DD"
          format="YYYY年MM月DD日"
          placeholder="选择日期"
          :disabled-date="disabledDate"
          @change="loadRooms"
        />
        <el-select
          v-model="filters.buildingId"
          clearable
          placeholder="全部楼栋"
          @change="loadRooms"
        >
          <el-option
            v-for="building in buildings"
            :key="building.id"
            :label="building.name"
            :value="building.id"
          />
        </el-select>
        <el-input-number
          v-model="filters.capacity"
          :min="1"
          :max="200"
          placeholder="参会人数"
          controls-position="right"
          @change="loadRooms"
        />
        <el-select
          v-model="filters.equipment"
          clearable
          placeholder="全部设备"
          @change="loadRooms"
        >
          <el-option
            v-for="item in equipmentOptions"
            :key="item.id"
            :label="item.name"
            :value="item.name"
          />
        </el-select>
        <el-button type="primary" @click="loadRooms">
          <Search :size="16" /> 查询
        </el-button>
      </div>
    </section>

    <section v-loading="loading" class="room-grid">
      <article v-for="room in rooms" :key="room.id" class="room-card">
        <div class="room-image-wrap">
          <img class="room-image" :src="room.imageUrl" :alt="room.name" />
          <StatusTag class="room-status" :status="room.status" />
          <span class="room-capacity">{{ room.capacity }} 人</span>
          <button
            class="favorite-button"
            :class="{ active: room.favorite }"
            type="button"
            :title="room.favorite ? '取消收藏' : '收藏常用会议室'"
            @click="toggleFavorite(room)"
          >
            <Star :size="18" :fill="room.favorite ? 'currentColor' : 'none'" />
          </button>
        </div>

        <div class="room-body">
          <div class="room-heading">
            <div>
              <h3>{{ room.name }}</h3>
              <p>
                <MapPin :size="14" />
                {{ room.location }}
              </p>
            </div>
          </div>

          <div class="room-meta">
            <span><Building2 :size="15" /> {{ room.buildingName }}</span>
            <span><UsersRound :size="15" /> 最多 {{ room.capacity }} 人</span>
            <span>
              <Clock3 :size="15" />
              {{ room.openTime.slice(0, 5) }}-{{ room.closeTime.slice(0, 5) }}
            </span>
          </div>

          <p class="room-description">{{ room.description }}</p>

          <div class="equipment-list">
            <span v-for="item in room.equipment" :key="item">
              <MonitorSmartphone :size="13" /> {{ item }}
            </span>
          </div>

          <div class="availability-block">
            <div class="availability-heading">
              <span><CalendarDays :size="15" /> 可用时段</span>
              <small>绿色可预约，黄色已占用，红色不可用</small>
            </div>
            <div class="slot-list">
              <button
                v-for="slot in room.availability"
                :key="`${slot.startTime}-${slot.endTime}`"
                class="slot-button"
                :class="slot.status.toLowerCase()"
                type="button"
                :disabled="slot.status !== 'AVAILABLE'"
                :title="slot.statusText"
                @click="openBooking(room, slot)"
              >
                {{ slot.startTime.slice(0, 5) }}
              </button>
            </div>
          </div>

          <el-button
            class="booking-button"
            type="primary"
            :disabled="!firstAvailableSlot(room) || !auth.user?.bookingAllowed"
            @click="openBooking(room)"
          >
            立即预约
          </el-button>
        </div>
      </article>

      <div v-if="!loading && !rooms.length" class="surface empty-state room-empty">
        <div>
          <Building2 :size="38" />
          <p>当前条件下没有匹配的会议室</p>
        </div>
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="`预约 ${selectedRoom?.name || ''}`"
      width="560px"
      destroy-on-close
    >
      <div v-if="selectedRoom" class="dialog-room">
        <img :src="selectedRoom.imageUrl" :alt="selectedRoom.name" />
        <div>
          <strong>{{ selectedRoom.location }}</strong>
          <p>{{ selectedRoom.buildingName }} · 最多 {{ selectedRoom.capacity }} 人</p>
        </div>
      </div>

      <el-form
        ref="formRef"
        :model="bookingForm"
        :rules="bookingRules"
        label-position="top"
      >
        <el-form-item label="会议主题" prop="subject">
          <el-input
            v-model="bookingForm.subject"
            maxlength="120"
            show-word-limit
            placeholder="请输入会议主题"
          />
        </el-form-item>
        <el-form-item label="预约时间" prop="timeRange">
          <el-date-picker
            v-model="bookingForm.timeRange"
            type="datetimerange"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            :disabled-date="disabledDate"
            style="width: 100%"
          />
        </el-form-item>
        <div class="dialog-grid">
          <el-form-item label="参会人数" prop="attendeeCount">
            <el-input-number
              v-model="bookingForm.attendeeCount"
              :min="1"
              :max="selectedRoom?.capacity || 500"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
          <div class="selected-equipment">
            <span>现场设备</span>
            <div>
              <el-tag
                v-for="item in selectedRoom?.equipment"
                :key="item"
                effect="plain"
                size="small"
              >
                {{ item }}
              </el-tag>
            </div>
          </div>
        </div>
        <el-form-item label="用途说明">
          <el-input
            v-model="bookingForm.purpose"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="简要说明会议用途，便于审批"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBooking">
          提交预约
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recommendVisible" title="智能会议室推荐" width="760px">
      <div class="recommend-intro">
        <Sparkles :size="20" />
        <div>
          <strong>按适配度排序</strong>
          <p>优先推荐容量匹配、设备满足、没有时间冲突且已收藏的会议室。</p>
        </div>
      </div>
      <el-form label-position="top">
        <div class="recommend-form">
          <el-form-item label="参会人数">
            <el-input-number
              v-model="recommendForm.attendeeCount"
              :min="1"
              :max="500"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="设备要求">
            <el-select
              v-model="recommendForm.equipment"
              clearable
              placeholder="不限制"
              style="width: 100%"
            >
              <el-option
                v-for="item in equipmentOptions"
                :key="item.id"
                :label="item.name"
                :value="item.name"
              />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="计划使用时间">
          <el-date-picker
            v-model="recommendForm.timeRange"
            type="datetimerange"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            :disabled-date="disabledDate"
            style="width: 100%"
          />
        </el-form-item>
        <el-button type="primary" :loading="recommendLoading" @click="loadRecommendations">
          <Sparkles :size="16" /> 生成推荐
        </el-button>
      </el-form>

      <div v-loading="recommendLoading" class="recommend-results">
        <article
          v-for="room in recommendations"
          :key="room.id"
          class="recommend-row"
        >
          <img :src="room.imageUrl" :alt="room.name" />
          <div>
            <div class="recommend-title">
              <strong>{{ room.name }}</strong>
              <el-tag type="success" effect="light">
                匹配度 {{ room.recommendationScore }}
              </el-tag>
            </div>
            <p>{{ room.buildingName }} · {{ room.location }}</p>
            <span>{{ room.capacity }} 人 · {{ room.equipment.join(' / ') || '无设备信息' }}</span>
          </div>
          <el-button type="primary" plain @click="openRecommendedBooking(room)">
            预约
          </el-button>
        </article>
        <div v-if="!recommendations.length" class="recommend-empty">
          <BadgeAlert :size="28" />
          <span>填写条件后生成推荐结果</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.room-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  min-height: 200px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.room-card {
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: white;
  box-shadow: var(--shadow-sm);
}

.room-image-wrap {
  position: relative;
  height: 184px;
  overflow: hidden;
  background: #e7ebf1;
}

.room-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.room-card:hover .room-image {
  transform: scale(1.025);
}

.room-status {
  position: absolute;
  top: 13px;
  left: 13px;
}

.room-capacity {
  position: absolute;
  right: 13px;
  bottom: 12px;
  padding: 5px 9px;
  border-radius: 6px;
  background: rgba(17, 28, 51, 0.84);
  color: white;
  font-size: 11px;
  font-weight: 700;
  backdrop-filter: blur(8px);
}

.favorite-button {
  position: absolute;
  top: 12px;
  right: 12px;
  display: grid;
  width: 35px;
  height: 35px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 50%;
  background: rgba(17, 28, 51, 0.62);
  color: white;
  cursor: pointer;
  backdrop-filter: blur(8px);
}

.favorite-button.active {
  color: #f6c344;
}

.room-body {
  padding: 17px;
}

.room-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.room-heading h3 {
  margin: 0;
  font-size: 17px;
}

.room-heading p {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 7px 0 0;
  color: var(--muted);
  font-size: 12px;
}

.room-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 15px;
  margin-top: 15px;
  color: #475467;
  font-size: 12px;
}

.room-meta span {
  display: flex;
  align-items: center;
  gap: 5px;
}

.room-description {
  min-height: 40px;
  margin: 14px 0 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.65;
}

.equipment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 13px;
}

.equipment-list span {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 7px;
  border-radius: 5px;
  background: #f3f5f8;
  color: #475467;
  font-size: 11px;
}

.availability-block {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
}

.availability-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.availability-heading span {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 700;
}

.availability-heading small {
  color: var(--muted);
  font-size: 10px;
}

.slot-list {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 5px;
  margin-top: 10px;
}

.slot-button {
  min-width: 0;
  height: 31px;
  padding: 0 2px;
  border: 1px solid #d5e3d9;
  border-radius: 5px;
  background: var(--green-soft);
  color: var(--green);
  font-size: 10px;
  cursor: pointer;
}

.slot-button:hover:not(:disabled) {
  border-color: var(--green);
}

.slot-button.occupied {
  border-color: #f0d6ac;
  background: var(--amber-soft);
  color: var(--amber);
}

.slot-button.unavailable {
  border-color: #efd4d7;
  background: var(--red-soft);
  color: var(--red);
}

.slot-button.past {
  border-color: #e1e5eb;
  background: #f4f5f7;
  color: #98a2b3;
}

.slot-button:disabled {
  cursor: not-allowed;
}

.booking-button {
  width: 100%;
  margin-top: 16px;
}

.room-empty {
  grid-column: 1 / -1;
}

.dialog-room {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
  padding: 10px;
  border: 1px solid var(--line);
  border-radius: 7px;
  background: #fafbfc;
}

.recommend-intro {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  margin-bottom: 16px;
  padding: 13px;
  border: 1px solid #dbe5f5;
  border-radius: 7px;
  background: #f8fbff;
  color: var(--blue);
}

.recommend-intro strong {
  color: var(--ink);
}

.recommend-intro p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 11px;
}

.recommend-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

.recommend-results {
  display: grid;
  gap: 9px;
  min-height: 120px;
  margin-top: 16px;
}

.recommend-row {
  display: grid;
  grid-template-columns: 68px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border: 1px solid var(--line);
  border-radius: 7px;
}

.recommend-row img {
  width: 68px;
  height: 52px;
  border-radius: 6px;
  object-fit: cover;
}

.recommend-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.recommend-row p {
  margin: 5px 0 0;
  color: var(--muted);
  font-size: 11px;
}

.recommend-row > div > span {
  display: block;
  margin-top: 4px;
  color: #475467;
  font-size: 10px;
}

.recommend-empty {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  min-height: 130px;
  color: var(--muted);
  font-size: 12px;
}

.dialog-room img {
  width: 72px;
  height: 58px;
  border-radius: 6px;
  object-fit: cover;
}

.dialog-room strong {
  font-size: 13px;
}

.dialog-room p {
  margin: 5px 0 0;
  color: var(--muted);
  font-size: 12px;
}

.dialog-grid {
  display: grid;
  grid-template-columns: minmax(150px, 0.7fr) minmax(0, 1.3fr);
  gap: 16px;
}

.selected-equipment > span {
  display: block;
  margin-bottom: 9px;
  color: #606266;
  font-size: 14px;
}

.selected-equipment > div {
  display: flex;
  min-height: 32px;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

@media (max-width: 1120px) {
  .room-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .room-image-wrap {
    height: 152px;
  }

  .slot-list {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .recommend-form {
    grid-template-columns: 1fr;
  }

  .recommend-row {
    grid-template-columns: 58px minmax(0, 1fr);
  }

  .recommend-row > .el-button {
    grid-column: 2;
    justify-self: start;
  }

  .dialog-grid {
    grid-template-columns: 1fr;
  }
}
</style>
