<template>
  <div>
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="就诊日期">
          <el-date-picker v-model="query.date" type="date" value-format="YYYY-MM-DD" placeholder="全部日期" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="待就诊" :value="0" />
            <el-option label="已完成" :value="1" />
            <el-option label="已取消" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <span>我的患者预约</span>
      </template>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="appointmentNo" label="预约单号" width="200" />
        <el-table-column prop="patientName" label="患者" width="110">
          <template #default="{ row }">
            {{ row.patientName }}
            <el-tooltip content="患者姓名已脱敏显示" placement="top">
              <el-icon style="color: #bbb; margin-left: 4px"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="appointmentDate" label="就诊日期" width="120" />
        <el-table-column prop="timeSlotDesc" label="时段" width="80" />
        <el-table-column prop="fee" label="挂号费" width="90" />
        <el-table-column prop="statusDesc" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button link type="success" @click="complete(row)">确认就诊</el-button>
              <el-button link type="primary" @click="writeRecord(row)">填写病历</el-button>
            </template>
            <template v-else-if="row.status === 1">
              <el-button link type="primary" @click="viewRecord(row)">查看病历</el-button>
            </template>
            <span v-else style="color: #bbb">-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="load"
      />
    </el-card>

    <!-- 填写/编辑病历 -->
    <el-dialog v-model="recordVisible" :title="recordForm.id ? '编辑病历' : '填写病历'" width="640px">
      <el-form :model="recordForm" :rules="recordRules" ref="recordFormRef" label-width="100px">
        <el-form-item label="就诊日期" prop="visitDate">
          <el-date-picker v-model="recordForm.visitDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="主诉" prop="chiefComplaint">
          <el-input v-model="recordForm.chiefComplaint" type="textarea" :rows="2" placeholder="患者自述症状" />
        </el-form-item>
        <el-form-item label="现病史" prop="presentIllness">
          <el-input v-model="recordForm.presentIllness" type="textarea" :rows="3" placeholder="发病过程、既往史等" />
        </el-form-item>
        <el-form-item label="诊断" prop="diagnosis">
          <el-input v-model="recordForm.diagnosis" placeholder="诊断结果" />
        </el-form-item>
        <el-form-item label="治疗方案" prop="treatmentPlan">
          <el-input v-model="recordForm.treatmentPlan" type="textarea" :rows="3" placeholder="治疗建议、用药指导等" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="recordForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRecord">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageDoctorAppointments, completeAppointment,
  getRecordByAppointment, saveRecord, updateRecord
} from '../api/clinic.js'

const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const recordVisible = ref(false)
const recordFormRef = ref()
const currentAppointment = ref(null)

const query = reactive({ pageNum: 1, pageSize: 10, status: null, date: null })

const recordForm = reactive({
  id: null,
  appointmentId: null,
  visitDate: '',
  chiefComplaint: '',
  presentIllness: '',
  diagnosis: '',
  treatmentPlan: '',
  remark: ''
})

const recordRules = {
  visitDate: [{ required: true, message: '请选择就诊日期', trigger: 'change' }],
  chiefComplaint: [{ required: true, message: '请填写主诉', trigger: 'blur' }],
  diagnosis: [{ required: true, message: '请填写诊断结果', trigger: 'blur' }]
}

const statusTag = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'info' }
  return map[status] ?? 'info'
}

const load = async () => {
  loading.value = true
  try {
    const data = await pageDoctorAppointments(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.status = null
  query.date = null
  query.pageNum = 1
  load()
}

const complete = async (row) => {
  await ElMessageBox.confirm(`确认 ${row.patientName} 已完成就诊？`, '提示', { type: 'info' })
  await completeAppointment(row.id)
  ElMessage.success('已确认就诊')
  load()
}

const writeRecord = async (row) => {
  currentAppointment.value = row
  const existing = await getRecordByAppointment(row.id)
  if (existing) {
    Object.assign(recordForm, {
      id: existing.id,
      appointmentId: row.id,
      visitDate: existing.visitDate,
      chiefComplaint: existing.chiefComplaint,
      presentIllness: existing.presentIllness,
      diagnosis: existing.diagnosis,
      treatmentPlan: existing.treatmentPlan,
      remark: existing.remark
    })
  } else {
    Object.assign(recordForm, {
      id: null,
      appointmentId: row.id,
      visitDate: row.appointmentDate,
      chiefComplaint: '',
      presentIllness: '',
      diagnosis: '',
      treatmentPlan: '',
      remark: ''
    })
  }
  recordVisible.value = true
}

const submitRecord = async () => {
  await recordFormRef.value.validate()
  saving.value = true
  try {
    if (recordForm.id) {
      await updateRecord(recordForm)
      ElMessage.success('病历已更新')
    } else {
      await saveRecord(recordForm)
      ElMessage.success('病历已保存')
    }
    recordVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const viewRecord = (row) => {
  router.push('/doctor-records')
}

onMounted(load)
</script>
