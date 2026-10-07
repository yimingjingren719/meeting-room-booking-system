<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TooltipComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  CanvasRenderer,
])

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    default: '',
  },
  option: {
    type: Object,
    required: true,
  },
  height: {
    type: Number,
    default: 300,
  },
})

const chartEl = ref(null)
let chart
let resizeObserver

function render() {
  if (!chartEl.value) return
  if (!chart) {
    chart = echarts.init(chartEl.value)
  }
  chart.setOption(props.option, true)
}

watch(
  () => props.option,
  () => nextTick(render),
  { deep: true },
)

onMounted(() => {
  render()
  resizeObserver = new ResizeObserver(() => chart?.resize())
  resizeObserver.observe(chartEl.value)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
})
</script>

<template>
  <section class="surface chart-panel">
    <header class="surface-header">
      <div>
        <h2 class="surface-title">{{ title }}</h2>
        <p v-if="description" class="chart-description">{{ description }}</p>
      </div>
    </header>
    <div ref="chartEl" class="chart-canvas" :style="{ height: `${height}px` }" />
  </section>
</template>

<style scoped>
.chart-panel {
  min-width: 0;
}

.chart-description {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 12px;
}

.chart-canvas {
  width: 100%;
  padding: 10px 12px 4px;
}
</style>
