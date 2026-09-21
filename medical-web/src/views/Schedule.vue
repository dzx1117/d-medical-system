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
            <el-option v-for="d in doctors" :key="d.id" :label="d.realName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="出诊日期">
          <el-date-picker v-model="query.scheduleDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="success" @click="openDialog()">新增排班</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="doctorName" label="医生" width="110" />
        <el-table-column prop="departmentName" label="科室" width="120" />
        <el-table-column prop="title" label="职称" width="120" />
        <el-table-column prop="scheduleDate" label="出诊日期" width="130" />
        <el-table-column prop="timeSlotDesc" label="时段" width="90" />
        <el-table-column label="号源" width="140">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.remainingSlots === 0 }">
              {{ row.bookedSlots }} / {{ row.totalSlots }}
            </span>
            <el-tag v-if="row.remainingSlots === 0" type="danger" size="small" style="margin-left: 6px">已约满</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fee" label="挂号费(元)" width="110" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '停诊' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="load"
        @current-change="load"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑排班' : '新增排班'" width="520px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="科室">
          <el-select v-model="form.departmentId" placeholder="先选科室" style="width: 100%" @change="onFormDeptChange">
            <el-option v-for="d in departments" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="医生" prop="doctorId">
          <el-select v-model="form.doctorId" placeholder="请选择医生" style="width: 100%">
            <el-option v-for="d in formDoctors" :key="d.id" :label="`${d.realName}（${d.title}）`" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="出诊日期" prop="scheduleDate">
          <el-date-picker v-model="form.scheduleDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="时段" prop="timeSlot">
          <el-radio-group v-model="form.timeSlot">
            <el-radio :value="1">上午</el-radio>
            <el-radio :value="2">下午</el-radio>
            <el-radio :value="3">晚上</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="号源数量" prop="totalSlots">
          <el-input-number v-model="form.totalSlots" :min="1" :max="200" />
        </el-form-item>
        <el-form-item label="挂号费(元)">
          <el-input-number v-model="form.fee" :min="0" :precision="2" :step="5" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停诊</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageSchedule, saveSchedule, updateSchedule, deleteSchedule, listDepartment, listDoctor } from '../api/basic.js'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const departments = ref([])
const doctors = ref([])
const formDoctors = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({ pageNum: 1, pageSize: 10, departmentId: null, doctorId: null, scheduleDate: null })

const form = reactive({
  id: null,
  departmentId: null,
  doctorId: null,
  scheduleDate: '',
  timeSlot: 1,
  totalSlots: 20,
  fee: 20,
  status: 1
})

const rules = {
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  scheduleDate: [{ required: true, message: '请选择出诊日期', trigger: 'change' }],
  timeSlot: [{ required: true, message: '请选择时段', trigger: 'change' }],
  totalSlots: [{ required: true, message: '请输入号源数量', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const data = await pageSchedule(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const onDeptChange = async () => {
  query.doctorId = null
  doctors.value = await listDoctor({ departmentId: query.departmentId })
}

const onFormDeptChange = async () => {
  form.doctorId = null
  formDoctors.value = await listDoctor({ departmentId: form.departmentId })
}

const reset = () => {
  Object.assign(query, { pageNum: 1, departmentId: null, doctorId: null, scheduleDate: null })
  doctors.value = []
  load()
}

const openDialog = async (row) => {
  if (row) {
    Object.assign(form, {
      id: row.id,
      departmentId: row.departmentId,
      doctorId: row.doctorId,
      scheduleDate: row.scheduleDate,
      timeSlot: row.timeSlot,
      totalSlots: row.totalSlots,
      fee: row.fee,
      status: row.status
    })
    formDoctors.value = await listDoctor({ departmentId: row.departmentId })
  } else {
    Object.assign(form, {
      id: null, departmentId: null, doctorId: null, scheduleDate: '',
      timeSlot: 1, totalSlots: 20, fee: 20, status: 1
    })
    formDoctors.value = []
  }
  dialogVisible.value = true
}

const submit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateSchedule(form)
      ElMessage.success('修改成功')
    } else {
      await saveSchedule(form)
      ElMessage.success('排班成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const remove = async (row) => {
  await ElMessageBox.confirm('确定删除该排班吗？', '提示', { type: 'warning' })
  await deleteSchedule(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  departments.value = await listDepartment()
  load()
})
</script>

<style scoped>
.text-danger {
  color: #f56c6c;
  font-weight: bold;
}
</style>
