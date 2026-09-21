<template>
  <div>
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="科室">
          <el-select v-model="query.departmentId" placeholder="全部科室" clearable style="width: 160px" @change="onDeptChange">
            <el-option v-for="d in departments" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="医生">
          <el-select v-model="query.doctorId" placeholder="全部医生" clearable style="width: 160px">
            <el-option v-for="d in doctors" :key="d.id" :label="`${d.realName}（${d.title}）`" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="就诊日期">
          <el-date-picker v-model="query.scheduleDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" :disabled-date="disablePast" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询号源</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <span>可预约号源</span>
      </template>

      <el-empty v-if="!loading && list.length === 0" description="暂无可预约号源" />

      <el-row :gutter="16">
        <el-col v-for="item in list" :key="item.id" :span="8" style="margin-bottom: 16px">
          <el-card shadow="hover" class="slot-card">
            <div class="slot-header">
              <span class="doctor-name">{{ item.doctorName }}</span>
              <el-tag size="small" type="warning">{{ item.title }}</el-tag>
            </div>
            <div class="slot-line">{{ item.departmentName }}</div>
            <div class="slot-line">
              {{ item.scheduleDate }} · {{ item.timeSlotDesc }}
            </div>
            <div class="slot-footer">
              <div>
                <span class="fee">¥{{ item.fee }}</span>
                <span class="remain">剩余 {{ item.remainingSlots }} / {{ item.totalSlots }}</span>
              </div>
              <el-button type="primary" size="small" @click="book(item)">预约</el-button>
            </div>
            <el-progress
              :percentage="Math.round((item.bookedSlots / item.totalSlots) * 100)"
              :stroke-width="6"
              :show-text="false"
              style="margin-top: 8px"
            />
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAvailableSchedule, listDepartment, listDoctor } from '../api/basic.js'
import { createAppointment } from '../api/clinic.js'

const loading = ref(false)
const list = ref([])
const departments = ref([])
const doctors = ref([])

const query = reactive({ departmentId: null, doctorId: null, scheduleDate: null })

const disablePast = (date) => date.getTime() < Date.now() - 86400000

const load = async () => {
  loading.value = true
  try {
    list.value = await listAvailableSchedule(query)
  } finally {
    loading.value = false
  }
}

const onDeptChange = async () => {
  query.doctorId = null
  doctors.value = await listDoctor({ departmentId: query.departmentId })
}

const reset = () => {
  Object.assign(query, { departmentId: null, doctorId: null, scheduleDate: null })
  doctors.value = []
  load()
}

const book = async (item) => {
  await ElMessageBox.confirm(
    `确认预约 ${item.scheduleDate} ${item.timeSlotDesc} ${item.doctorName} 医生的号？挂号费 ¥${item.fee}`,
    '预约确认',
    { type: 'info' }
  )
  await createAppointment(item.id)
  ElMessage.success('预约成功')
  load()
}

onMounted(async () => {
  departments.value = await listDepartment()
  load()
})
</script>

<style scoped>
.slot-card {
  border-left: 4px solid #1a5f7a;
}

.slot-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.doctor-name {
  font-size: 16px;
  font-weight: bold;
  color: #1a5f7a;
}

.slot-line {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
}

.slot-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.fee {
  font-size: 16px;
  font-weight: bold;
  color: #f56c6c;
  margin-right: 10px;
}

.remain {
  font-size: 12px;
  color: #999;
}
</style>
