<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: {
    type: String,
    required: true,
  },
  text: {
    type: String,
    default: '',
  },
})

const statusMap = {
  PENDING: { label: '待审批', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  CANCELLED: { label: '已取消', type: 'info' },
  COMPLETED: { label: '已完成', type: 'info' },
  EXPIRED: { label: '已过期', type: 'info' },
  AVAILABLE: { label: '可预约', type: 'success' },
  OCCUPIED: { label: '已占用', type: 'warning' },
  UNAVAILABLE: { label: '暂停开放', type: 'danger' },
  PAST: { label: '已结束', type: 'info' },
  MAINTENANCE: { label: '维护中', type: 'warning' },
  DISABLED: { label: '已停用', type: 'danger' },
}

const config = computed(
  () => statusMap[props.status] || { label: props.status, type: 'info' },
)
</script>

<template>
  <el-tag :type="config.type" effect="light" size="small">
    {{ text || config.label }}
  </el-tag>
</template>

