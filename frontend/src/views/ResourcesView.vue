<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Building2,
  CircleDot,
  MonitorSmartphone,
  Pencil,
  Power,
  Plus,
  Settings2,
  Trash2,
  Wrench,
} from '@lucide/vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, roomApi } from '../api'
import { formatDateTimeRange } from '../utils/format'

const loading = ref(false)
const submitting = ref(false)
const activeTab = ref('rooms')
const rooms = ref([])
const buildings = ref([])
const equipment = ref([])
const unavailableVisible = ref(false)
const unavailableRecords = ref([])
const selectedRoom = ref(null)
const roomDialogVisible = ref(false)
const editingRoomId = ref(null)
const roomFormRef = ref()

const unavailableForm = reactive({
  timeRange: [],
  reason: '',
})

const roomForm = reactive({
  buildingId: null,
  name: '',
  floorNo: 1,
  location: '',
  capacity: 10,
  openTime: '08:00:00',
  closeTime: '21:00:00',
  status: 'AVAILABLE',
  imageUrl: '/rooms/room-301.jpg',
  description: '',
  equipmentIds: [],
})

const roomRules = {
  buildingId: [{ required: true, message: '请选择楼栋', trigger: 'change' }],
  name: [{ required: true, message: '请输入会议室名称', trigger: 'blur' }],
  floorNo: [{ required: true, message: '请输入楼层', trigger: 'change' }],
  location: [{ required: true, message: '请输入具体位置', trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入容量', trigger: 'change' }],
  openTime: [{ required: true, message: '请选择开放时间', trigger: 'change' }],
  closeTime: [{ required: true, message: '请选择关闭时间', trigger: 'change' }],
}

const roomStatusOptions = [
  { label: '可预约', value: 'AVAILABLE' },
  { label: '维护中', value: 'MAINTENANCE' },
  { label: '已停用', value: 'DISABLED' },
]

const summary = computed(() => ({
  total: rooms.value.length,
  available: rooms.value.filter((room) => room.status === 'AVAILABLE').length,
  unavailable: rooms.value.filter((room) => room.status !== 'AVAILABLE').length,
  capacity: rooms.value.reduce((sum, room) => sum + room.capacity, 0),
}))

async function load() {
  loading.value = true
  try {
    const [roomData, buildingData, equipmentData, unavailableData] = await Promise.all([
      adminApi.rooms(),
      roomApi.buildings(),
      roomApi.equipment(),
      adminApi.unavailableRooms(),
    ])
    rooms.value = roomData
    buildings.value = buildingData
    equipment.value = equipmentData
    unavailableRecords.value = unavailableData
  } finally {
    loading.value = false
  }
}

function openRoomDialog(room = null) {
  editingRoomId.value = room?.id || null
  const equipmentIds = room
    ? room.equipment
        .map((name) => equipment.value.find((item) => item.name === name)?.id)
        .filter(Boolean)
    : []
  Object.assign(roomForm, {
    buildingId: room?.buildingId || buildings.value[0]?.id || null,
    name: room?.name || '',
    floorNo: room?.floorNo || 1,
    location: room?.location || '',
    capacity: room?.capacity || 10,
    openTime: room?.openTime || '08:00:00',
    closeTime: room?.closeTime || '21:00:00',
    status: room?.status || 'AVAILABLE',
    imageUrl: room?.imageUrl || '/rooms/room-301.jpg',
    description: room?.description || '',
    equipmentIds,
  })
  roomDialogVisible.value = true
}

async function saveRoom() {
  const valid = await roomFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editingRoomId.value) {
      await adminApi.updateRoom(editingRoomId.value, roomForm)
      ElMessage.success('会议室信息已更新')
    } else {
      await adminApi.createRoom(roomForm)
      ElMessage.success('会议室已创建')
    }
    roomDialogVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function disableRoom(room) {
  try {
    await ElMessageBox.confirm(
      `确认停用“${room.name}”吗？停用后普通用户将无法看到该会议室。`,
      '停用会议室',
      {
        type: 'warning',
        confirmButtonText: '确认停用',
        cancelButtonText: '返回',
      },
    )
    await adminApi.disableRoom(room.id)
    ElMessage.success('会议室已停用')
    await load()
  } catch {
    // Cancelled by the administrator.
  }
}

async function updateStatus(room, status) {
  const previous = room.status
  room.status = status
  try {
    await adminApi.updateRoomStatus(room.id, status)
    ElMessage.success('会议室状态已更新')
  } catch {
    room.status = previous
  }
}

function openUnavailable(room) {
  selectedRoom.value = room
  unavailableForm.timeRange = []
  unavailableForm.reason = ''
  unavailableVisible.value = true
}

async function submitUnavailable() {
  if (!selectedRoom.value) return
  if (unavailableForm.timeRange.length !== 2 || !unavailableForm.reason.trim()) {
    ElMessage.warning('请完整填写维护时间和原因')
    return
  }
  submitting.value = true
  try {
    const record = await adminApi.createUnavailable({
      roomId: selectedRoom.value.id,
      startTime: unavailableForm.timeRange[0],
      endTime: unavailableForm.timeRange[1],
      reason: unavailableForm.reason.trim(),
    })
    unavailableRecords.value.unshift({
      ...record,
      roomName: selectedRoom.value.name,
    })
    ElMessage.success('维护时段已创建')
    unavailableVisible.value = false
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="page">
    <header class="page-header">
      <div>
        <h2 class="page-title">资源维护</h2>
        <p class="page-subtitle">维护会议室开放状态、设备配置和不可预约时段。</p>
      </div>
      <div class="header-actions">
        <el-button @click="load"><Settings2 :size="16" /> 刷新资源</el-button>
        <el-button type="primary" @click="openRoomDialog()">
          <Plus :size="16" /> 新增会议室
        </el-button>
      </div>
    </header>

    <section class="metric-grid">
      <article class="metric" style="--metric-color: #2563eb">
        <div class="metric-label">会议室总数</div>
        <div class="metric-value">{{ summary.total }}</div>
        <div class="metric-note"><Building2 :size="14" /> 两个校区楼栋</div>
      </article>
      <article class="metric" style="--metric-color: #247a4b">
        <div class="metric-label">可预约</div>
        <div class="metric-value">{{ summary.available }}</div>
        <div class="metric-note"><CircleDot :size="14" /> 当前开放使用</div>
      </article>
      <article class="metric" style="--metric-color: #d97706">
        <div class="metric-label">非开放状态</div>
        <div class="metric-value">{{ summary.unavailable }}</div>
        <div class="metric-note"><Power :size="14" /> 维护或停用</div>
      </article>
      <article class="metric" style="--metric-color: #0f8b8d">
        <div class="metric-label">总座位数</div>
        <div class="metric-value">{{ summary.capacity }}</div>
        <div class="metric-note">所有会议室容量合计</div>
      </article>
    </section>

    <section class="surface">
      <div class="resource-tabs">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="会议室状态" name="rooms" />
          <el-tab-pane label="维护时段" name="maintenance" />
          <el-tab-pane label="设备目录" name="equipment" />
        </el-tabs>
      </div>

      <div v-if="activeTab === 'rooms'" class="resource-content">
        <el-table :data="rooms" style="width: 100%" row-key="id">
          <el-table-column label="会议室" min-width="230">
            <template #default="{ row }">
              <div class="room-cell">
                <img :src="row.imageUrl" :alt="row.name" />
                <div>
                  <strong>{{ row.name }}</strong>
                  <span>{{ row.buildingName }} · {{ row.location }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="capacity" label="容量" width="90" align="center" />
          <el-table-column label="开放时间" width="130">
            <template #default="{ row }">
              {{ row.openTime.slice(0, 5) }}-{{ row.closeTime.slice(0, 5) }}
            </template>
          </el-table-column>
          <el-table-column label="设备" min-width="220">
            <template #default="{ row }">
              <div class="tag-list">
                <el-tag
                  v-for="item in row.equipment"
                  :key="item"
                  size="small"
                  effect="plain"
                >
                  {{ item }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="155">
            <template #default="{ row }">
              <el-select
                :model-value="row.status"
                size="small"
                @change="(value) => updateStatus(row, value)"
              >
                <el-option
                  v-for="item in roomStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openRoomDialog(row)">
                <Pencil :size="15" /> 编辑
              </el-button>
              <el-button text type="primary" @click="openUnavailable(row)">
                <Wrench :size="15" /> 设置维护
              </el-button>
              <el-button
                v-if="row.status !== 'DISABLED'"
                text
                type="danger"
                @click="disableRoom(row)"
              >
                <Trash2 :size="15" /> 停用
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-else-if="activeTab === 'maintenance'" class="resource-content">
        <div class="maintenance-head">
          <div>
            <h3>不可预约时段</h3>
            <p>用于维修、考试或临时活动，与预约冲突检测共同生效。</p>
          </div>
          <el-button
            type="primary"
            :disabled="!rooms.length"
            @click="openUnavailable(rooms[0])"
          >
            <Wrench :size="16" /> 新增维护时段
          </el-button>
        </div>
        <div v-if="unavailableRecords.length" class="maintenance-list">
          <article
            v-for="record in unavailableRecords"
            :key="record.id"
            class="maintenance-row"
          >
            <span class="maintenance-icon"><Wrench :size="17" /></span>
            <div>
              <strong>{{ record.roomName }}</strong>
              <p>{{ record.reason }}</p>
            </div>
            <span>{{ formatDateTimeRange(record.startTime, record.endTime) }}</span>
          </article>
        </div>
        <div v-else class="empty-state">
          <div>
            <Wrench :size="36" />
            <p>本次会话尚未新增维护时段</p>
          </div>
        </div>
      </div>

      <div v-else class="resource-content">
        <div class="equipment-grid">
          <article v-for="item in equipment" :key="item.id" class="equipment-item">
            <span><MonitorSmartphone :size="20" /></span>
            <div>
              <strong>{{ item.name }}</strong>
              <p>设备编号 EQ-{{ String(item.id).padStart(3, '0') }}</p>
            </div>
            <el-tag type="success" effect="light" size="small">启用</el-tag>
          </article>
        </div>
      </div>
    </section>

    <el-dialog v-model="unavailableVisible" title="新增维护时段" width="520px">
      <div v-if="selectedRoom" class="maintenance-dialog-room">
        <strong>{{ selectedRoom.name }}</strong>
        <span>{{ selectedRoom.location }}</span>
      </div>
      <el-form label-position="top">
        <el-form-item label="维护时间" required>
          <el-date-picker
            v-model="unavailableForm.timeRange"
            type="datetimerange"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="维护原因" required>
          <el-input
            v-model="unavailableForm.reason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="例如：投影设备检修、考试封闭使用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="unavailableVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitUnavailable">
          确认创建
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="roomDialogVisible"
      :title="editingRoomId ? '编辑会议室' : '新增会议室'"
      width="640px"
      destroy-on-close
    >
      <el-form
        ref="roomFormRef"
        :model="roomForm"
        :rules="roomRules"
        label-position="top"
      >
        <div class="dialog-grid">
          <el-form-item label="会议室名称" prop="name">
            <el-input v-model="roomForm.name" maxlength="80" />
          </el-form-item>
          <el-form-item label="所属楼栋" prop="buildingId">
            <el-select v-model="roomForm.buildingId" style="width: 100%">
              <el-option
                v-for="building in buildings"
                :key="building.id"
                :label="building.name"
                :value="building.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="楼层" prop="floorNo">
            <el-input-number
              v-model="roomForm.floorNo"
              :min="-5"
              :max="120"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="容量" prop="capacity">
            <el-input-number
              v-model="roomForm.capacity"
              :min="1"
              :max="500"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="开放时间" prop="openTime">
            <el-time-picker
              v-model="roomForm.openTime"
              value-format="HH:mm:ss"
              format="HH:mm"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="关闭时间" prop="closeTime">
            <el-time-picker
              v-model="roomForm.closeTime"
              value-format="HH:mm:ss"
              format="HH:mm"
              style="width: 100%"
            />
          </el-form-item>
        </div>
        <el-form-item label="具体位置" prop="location">
          <el-input v-model="roomForm.location" maxlength="120" />
        </el-form-item>
        <el-form-item label="设备">
          <el-select
            v-model="roomForm.equipmentIds"
            multiple
            collapse-tags
            style="width: 100%"
          >
            <el-option
              v-for="item in equipment"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="会议室状态">
          <el-radio-group v-model="roomForm.status">
            <el-radio-button
              v-for="item in roomStatusOptions"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="图片路径">
          <el-input v-model="roomForm.imageUrl" placeholder="/rooms/room-301.jpg" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="roomForm.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roomDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="saveRoom">
          保存会议室
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.resource-tabs {
  padding: 0 20px;
  border-bottom: 1px solid var(--line);
}

.header-actions {
  display: flex;
  gap: 10px;
}

.resource-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.resource-content {
  min-height: 360px;
  overflow-x: auto;
}

.room-cell {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  align-items: center;
  gap: 11px;
}

.room-cell img {
  width: 58px;
  height: 45px;
  border-radius: 6px;
  object-fit: cover;
}

.room-cell div {
  display: grid;
}

.room-cell strong {
  font-size: 13px;
}

.room-cell span {
  margin-top: 4px;
  color: var(--muted);
  font-size: 11px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.maintenance-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 20px;
  border-bottom: 1px solid var(--line);
}

.maintenance-head h3 {
  margin: 0;
  font-size: 16px;
}

.maintenance-head p {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 12px;
}

.maintenance-list {
  display: grid;
}

.maintenance-row {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--line);
}

.maintenance-icon,
.equipment-item > span {
  display: grid;
  place-items: center;
  border-radius: 7px;
  background: var(--amber-soft);
  color: var(--amber);
}

.maintenance-icon {
  width: 36px;
  height: 36px;
}

.maintenance-row strong {
  font-size: 13px;
}

.maintenance-row p {
  margin: 5px 0 0;
  color: var(--muted);
  font-size: 11px;
}

.maintenance-row > span:last-child {
  color: var(--muted);
  font-size: 11px;
}

.equipment-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 20px;
}

.equipment-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 15px;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.equipment-item > span {
  width: 40px;
  height: 40px;
  background: var(--blue-soft);
  color: var(--blue);
}

.equipment-item strong {
  font-size: 13px;
}

.equipment-item p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 10px;
  font-family: ui-monospace, "SFMono-Regular", Consolas, monospace;
}

.maintenance-dialog-room {
  display: grid;
  gap: 5px;
  margin-bottom: 18px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 7px;
  background: #fafbfc;
}

.maintenance-dialog-room span {
  color: var(--muted);
  font-size: 12px;
}

@media (max-width: 760px) {
  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .equipment-grid {
    grid-template-columns: 1fr;
  }

  .maintenance-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .maintenance-row {
    grid-template-columns: 36px minmax(0, 1fr);
  }

  .maintenance-row > span:last-child {
    grid-column: 2;
  }
}
</style>
