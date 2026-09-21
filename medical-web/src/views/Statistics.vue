<template>
  <div v-loading="loading">
    <!-- 核心指标 -->
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-sub">{{ card.sub }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <!-- 近 7 天预约趋势 -->
      <el-col :span="24">
        <el-card shadow="never">
          <template #header><span>未来 7 天就诊预约分布</span></template>
          <div class="trend">
            <div v-for="p in trend" :key="p.name" class="trend-item">
              <div class="trend-count">{{ p.value }}</div>
              <div class="trend-bar" :style="{ height: barHeight(p.value, maxTrend) }"></div>
              <div class="trend-label">{{ p.name.slice(5) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <!-- 科室就诊量 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>科室就诊量排行</span></template>
          <el-empty v-if="deptRanking.length === 0" description="暂无已完成就诊数据" :image-size="60" />
          <div v-for="item in deptRanking" :key="item.name" class="rank-row">
            <div class="rank-name">{{ item.name }}</div>
            <div class="rank-track">
              <div class="rank-fill blue" :style="{ width: pct(item.value, maxDept) }"></div>
            </div>
            <div class="rank-value">{{ item.value }}</div>
          </div>
        </el-card>
      </el-col>

      <!-- 医生工作量 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>医生工作量排行</span></template>
          <el-empty v-if="doctorRanking.length === 0" description="暂无已完成就诊数据" :image-size="60" />
          <div v-for="item in doctorRanking" :key="item.name" class="rank-row">
            <div class="rank-name">{{ item.name }}</div>
            <div class="rank-track">
              <div class="rank-fill green" :style="{ width: pct(item.value, maxDoctor) }"></div>
            </div>
            <div class="rank-value">{{ item.value }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <!-- 时段分布 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>预约时段分布</span></template>
          <el-empty v-if="timeSlots.length === 0" description="暂无预约数据" :image-size="60" />
          <div v-for="item in timeSlots" :key="item.name" class="rank-row">
            <div class="rank-name">{{ item.name }}</div>
            <div class="rank-track">
              <div class="rank-fill orange" :style="{ width: pct(item.value, maxSlot) }"></div>
            </div>
            <div class="rank-value">{{ item.value }}</div>
          </div>
        </el-card>
      </el-col>

      <!-- 预约状态构成 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>预约状态构成</span></template>
          <div v-for="s in statusList" :key="s.label" class="rank-row">
            <div class="rank-name">{{ s.label }}</div>
            <div class="rank-track">
              <div class="rank-fill" :style="{ width: pct(s.value, maxStatus), background: s.color }"></div>
            </div>
            <div class="rank-value">{{ s.value }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  getOverview, getDepartmentRanking, getDoctorRanking, getUpcomingTrend, getTimeSlot
} from '../api/statistics.js'

const loading = ref(false)
const overview = ref({})
const deptRanking = ref([])
const doctorRanking = ref([])
const trend = ref([])
const timeSlots = ref([])

const cards = computed(() => [
  { label: '预约总数', value: overview.value.totalAppointments ?? 0, color: '#1a5f7a', sub: `今日 ${overview.value.todayAppointments ?? 0} 人就诊` },
  { label: '待就诊', value: overview.value.pendingAppointments ?? 0, color: '#e6a23c', sub: `已完成 ${overview.value.completedAppointments ?? 0}` },
  { label: '已取消', value: overview.value.cancelledAppointments ?? 0, color: '#909399', sub: `病历 ${overview.value.totalRecords ?? 0} 份` },
  { label: '处方总金额', value: `¥${overview.value.totalPrescriptionAmount ?? 0}`, color: '#f56c6c', sub: `${overview.value.totalPrescriptions ?? 0} 张处方` }
])

const statusList = computed(() => [
  { label: '待就诊', value: overview.value.pendingAppointments ?? 0, color: '#e6a23c' },
  { label: '已完成', value: overview.value.completedAppointments ?? 0, color: '#67c23a' },
  { label: '已取消', value: overview.value.cancelledAppointments ?? 0, color: '#909399' }
])

const maxTrend = computed(() => Math.max(1, ...trend.value.map(i => i.value)))
const maxDept = computed(() => Math.max(1, ...deptRanking.value.map(i => i.value)))
const maxDoctor = computed(() => Math.max(1, ...doctorRanking.value.map(i => i.value)))
const maxSlot = computed(() => Math.max(1, ...timeSlots.value.map(i => i.value)))
const maxStatus = computed(() => Math.max(1, ...statusList.value.map(i => i.value)))

const pct = (v, max) => `${Math.round((v / max) * 100)}%`
const barHeight = (v, max) => `${Math.max(4, Math.round((v / max) * 120))}px`

const load = async () => {
  loading.value = true
  try {
    const [o, d, doc, t, ts] = await Promise.all([
      getOverview(), getDepartmentRanking(), getDoctorRanking(), getUpcomingTrend(), getTimeSlot()
    ])
    overview.value = o
    deptRanking.value = d
    doctorRanking.value = doc
    trend.value = t
    timeSlots.value = ts
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.stat-card {
  border-left: 4px solid #1a5f7a;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  margin: 8px 0 4px;
}

.stat-sub {
  font-size: 12px;
  color: #b0b4bb;
}

.trend {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 180px;
  padding: 0 8px;
}

.trend-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  flex: 1;
}

.trend-count {
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;
}

.trend-bar {
  width: 60%;
  max-width: 44px;
  background: linear-gradient(180deg, #2a8fa8, #1a5f7a);
  border-radius: 4px 4px 0 0;
  transition: height 0.3s ease;
}

.trend-label {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.rank-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.rank-name {
  width: 90px;
  font-size: 13px;
  color: #606266;
  text-align: right;
  flex-shrink: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-track {
  flex: 1;
  height: 16px;
  background: #f0f2f5;
  border-radius: 8px;
  overflow: hidden;
}

.rank-fill {
  height: 100%;
  border-radius: 8px;
  transition: width 0.4s ease;
}

.rank-fill.blue {
  background: linear-gradient(90deg, #4a9dbb, #1a5f7a);
}

.rank-fill.green {
  background: linear-gradient(90deg, #7fd18f, #52a35f);
}

.rank-fill.orange {
  background: linear-gradient(90deg, #f0b45c, #d98c1f);
}

.rank-value {
  width: 40px;
  font-size: 13px;
  color: #303133;
  font-weight: bold;
}
</style>
